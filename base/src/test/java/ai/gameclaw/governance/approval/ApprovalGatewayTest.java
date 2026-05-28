package ai.gameclaw.governance.approval;

import ai.gameclaw.observability.AuditLogger;
import ai.gameclaw.security.RiskLevel;
import ai.gameclaw.security.Role;
import ai.gameclaw.security.TenantContext;
import ai.gameclaw.security.TenantContextHolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApprovalGatewayTest {

    private ApprovalGateway gateway;
    private JdbcTemplate jdbc;
    private ObjectMapper objectMapper;
    private ObjectProvider<AuditLogger> auditLoggerProvider;

    @BeforeEach
    void setUp() {
        jdbc = mock(JdbcTemplate.class);
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        auditLoggerProvider = mock(ObjectProvider.class);
        when(auditLoggerProvider.getIfAvailable()).thenReturn(null);
        ObjectProvider<ObjectMapper> omProvider = mock(ObjectProvider.class);
        when(omProvider.getIfAvailable(any())).thenReturn(objectMapper);
        gateway = new ApprovalGateway(jdbc, omProvider, auditLoggerProvider);
    }

    @Test
    void createInsertsPendingApproval() {
        UUID requesterId = UUID.randomUUID();
        TenantContext ctx = TenantContext.of(UUID.randomUUID(), UUID.randomUUID(), requesterId, Set.of(Role.PROGRAMMER));

        TenantContextHolder.runWith(ctx, () -> {
            ApprovalRequest request = new ApprovalRequest(
                    requesterId, "monster-config", "update",
                    RiskLevel.L3_PROJECT_WRITE, "3 files affected", Map.of(), 1, Duration.ofHours(1));

            PendingApproval approval = gateway.create(request);

            assertThat(approval).isNotNull();
            assertThat(approval.requesterId()).isEqualTo(requesterId);
            assertThat(approval.resource()).isEqualTo("monster-config");
            assertThat(approval.action()).isEqualTo("update");
            assertThat(approval.riskLevel()).isEqualTo(RiskLevel.L3_PROJECT_WRITE);
            assertThat(approval.state()).isEqualTo(ApprovalState.PENDING);
            assertThat(approval.quorum()).isEqualTo(1);
        });
    }

    @Test
    void approveMeetsQuorumTransitionsToApproved() {
        UUID approvalId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        PendingApproval pending = new PendingApproval(
                approvalId, UUID.randomUUID(), UUID.randomUUID(), "res", "act",
                RiskLevel.L3_PROJECT_WRITE, "", Map.of(), 1, List.of(),
                ApprovalState.PENDING, Instant.now().plus(1, ChronoUnit.HOURS), Instant.now());

        when(jdbc.query(anyString(), any(RowMapper.class), eq(approvalId)))
                .thenReturn(List.of(pending));

        PendingApproval result = gateway.approve(approvalId, userId, "ok");

        assertThat(result.state()).isEqualTo(ApprovalState.APPROVED);
    }

    @Test
    void approveBelowQuorumStaysPending() {
        UUID approvalId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        PendingApproval pending = new PendingApproval(
                approvalId, UUID.randomUUID(), UUID.randomUUID(), "res", "act",
                RiskLevel.L5_PRODUCTION, "", Map.of(), 2, List.of(),
                ApprovalState.PENDING, Instant.now().plus(1, ChronoUnit.HOURS), Instant.now());

        when(jdbc.query(anyString(), any(RowMapper.class), eq(approvalId)))
                .thenReturn(List.of(pending));

        PendingApproval result = gateway.approve(approvalId, userId, "ok");

        assertThat(result.state()).isEqualTo(ApprovalState.PENDING);
    }

    @Test
    void rejectTransitionsToRejected() {
        UUID approvalId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        PendingApproval pending = new PendingApproval(
                approvalId, UUID.randomUUID(), UUID.randomUUID(), "res", "act",
                RiskLevel.L3_PROJECT_WRITE, "", Map.of(), 1, List.of(),
                ApprovalState.PENDING, Instant.now().plus(1, ChronoUnit.HOURS), Instant.now());

        when(jdbc.query(anyString(), any(RowMapper.class), eq(approvalId)))
                .thenReturn(List.of(pending));

        PendingApproval result = gateway.reject(approvalId, userId, "bad");

        assertThat(result.state()).isEqualTo(ApprovalState.REJECTED);
    }

    @Test
    void expireOverdueReturnsCount() {
        when(jdbc.update(anyString(), eq(ApprovalState.EXPIRED.name()), eq(ApprovalState.PENDING.name())))
                .thenReturn(3);

        int result = gateway.expireOverdue();

        assertThat(result).isEqualTo(3);
    }

    @Test
    void emergencyOverrideRequiresAdminRole() {
        UUID approvalId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        PendingApproval pending = new PendingApproval(
                approvalId, UUID.randomUUID(), UUID.randomUUID(), "res", "act",
                RiskLevel.L5_PRODUCTION, "", Map.of(), 2, List.of(),
                ApprovalState.PENDING, Instant.now().plus(1, ChronoUnit.HOURS), Instant.now());

        when(jdbc.query(anyString(), any(RowMapper.class), eq(approvalId))).thenReturn(List.of(pending));

        TenantContext nonAdminCtx = TenantContext.of(
                UUID.randomUUID(), UUID.randomUUID(), adminId, Set.of(Role.PROGRAMMER));

        assertThatThrownBy(() -> TenantContextHolder.runWith(nonAdminCtx, () -> {
            gateway.emergencyOverride(approvalId, adminId, "urgent");
            return null;
        })).isInstanceOf(SecurityException.class)
                .hasMessageContaining("ADMIN");
    }

    @Test
    void emergencyOverrideRequiresReason() {
        UUID approvalId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        PendingApproval pending = new PendingApproval(
                approvalId, UUID.randomUUID(), UUID.randomUUID(), "res", "act",
                RiskLevel.L5_PRODUCTION, "", Map.of(), 2, List.of(),
                ApprovalState.PENDING, Instant.now().plus(1, ChronoUnit.HOURS), Instant.now());

        when(jdbc.query(anyString(), any(RowMapper.class), eq(approvalId))).thenReturn(List.of(pending));

        TenantContext adminCtx = TenantContext.of(
                UUID.randomUUID(), UUID.randomUUID(), adminId, Set.of(Role.ADMIN));

        assertThatThrownBy(() -> TenantContextHolder.runWith(adminCtx, () -> {
            gateway.emergencyOverride(approvalId, adminId, "");
            return null;
        })).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reason");
    }

    @Test
    void findByIdReturnsOptional() {
        UUID approvalId = UUID.randomUUID();
        PendingApproval approval = new PendingApproval(
                approvalId, UUID.randomUUID(), UUID.randomUUID(), "res", "act",
                RiskLevel.L3_PROJECT_WRITE, "", Map.of(), 1, List.of(),
                ApprovalState.PENDING, Instant.now().plus(1, ChronoUnit.HOURS), Instant.now());

        when(jdbc.query(anyString(), any(RowMapper.class), eq(approvalId))).thenReturn(List.of(approval));

        Optional<PendingApproval> result = gateway.findById(approvalId);

        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(approvalId);
    }

    @Test
    void findPendingReturnsOnlyPending() {
        UUID tenantId = UUID.randomUUID();
        PendingApproval approval = new PendingApproval(
                UUID.randomUUID(), tenantId, UUID.randomUUID(), "res", "act",
                RiskLevel.L3_PROJECT_WRITE, "", Map.of(), 1, List.of(),
                ApprovalState.PENDING, Instant.now().plus(1, ChronoUnit.HOURS), Instant.now());

        when(jdbc.query(anyString(), any(RowMapper.class), eq(tenantId), eq(ApprovalState.PENDING.name())))
                .thenReturn(List.of(approval));

        List<PendingApproval> result = gateway.findPending(tenantId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).state()).isEqualTo(ApprovalState.PENDING);
    }
}
