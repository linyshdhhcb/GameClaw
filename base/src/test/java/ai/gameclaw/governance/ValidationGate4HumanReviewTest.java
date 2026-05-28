package ai.gameclaw.governance;

import ai.gameclaw.governance.approval.ApprovalGateway;
import ai.gameclaw.governance.approval.ApprovalNotifier;
import ai.gameclaw.governance.approval.ApprovalRequest;
import ai.gameclaw.governance.approval.ApprovalState;
import ai.gameclaw.governance.approval.PendingApproval;
import ai.gameclaw.security.RiskLevel;
import ai.gameclaw.security.Role;
import ai.gameclaw.security.TenantContext;
import ai.gameclaw.security.TenantContextHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ValidationGate4HumanReviewTest {

    private ApprovalGateway approvalGateway;
    private ApprovalNotifier notifier;
    private ValidationGate4HumanReview gate;

    @BeforeEach
    void setUp() {
        approvalGateway = mock(ApprovalGateway.class);
        notifier = mock(ApprovalNotifier.class);
        gate = new ValidationGate4HumanReview(approvalGateway, notifier);
    }

    @Test
    void nameReturnsHumanReview() {
        assertThat(gate.name()).isEqualTo("human_review");
    }

    @Test
    void passesWhenNoApprovalRequired() {
        Map<String, Object> output = Map.of("action", "read", "data", "value");

        ValidationResult result = gate.validate(output, Map.class);

        assertThat(result.valid()).isTrue();
    }

    @Test
    void failsWhenApprovalRequired() {
        UUID approvalId = UUID.randomUUID();
        PendingApproval mockApproval = new PendingApproval(
                approvalId, UUID.randomUUID(), UUID.randomUUID(), "res", "act",
                RiskLevel.L3_PROJECT_WRITE, "", Map.of(), 1, List.of(),
                ApprovalState.PENDING, Instant.now().plus(1, ChronoUnit.HOURS), Instant.now());

        when(approvalGateway.create(any(ApprovalRequest.class))).thenReturn(mockApproval);

        TenantContext ctx = TenantContext.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), Set.of(Role.PROGRAMMER));
        TenantContextHolder.runWith(ctx, () -> {
            Map<String, Object> output = Map.of(
                    "requiresApproval", true,
                    "resource", "monster-config",
                    "action", "update",
                    "riskLevel", "L3_PROJECT_WRITE"
            );

            ValidationResult result = gate.validate(output, Map.class);

            assertThat(result.valid()).isFalse();
            assertThat(result.code()).isEqualTo("HUMAN_REVIEW_REQUIRED");
        });
    }

    @Test
    void createsApprovalRequestFromMap() {
        UUID approvalId = UUID.randomUUID();
        PendingApproval mockApproval = new PendingApproval(
                approvalId, UUID.randomUUID(), UUID.randomUUID(), "monster-config", "update",
                RiskLevel.L3_PROJECT_WRITE, "5 files affected", Map.of(), 2, List.of(),
                ApprovalState.PENDING, Instant.now().plus(1, ChronoUnit.HOURS), Instant.now());

        when(approvalGateway.create(any(ApprovalRequest.class))).thenReturn(mockApproval);

        TenantContext ctx = TenantContext.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), Set.of(Role.PROGRAMMER));
        TenantContextHolder.runWith(ctx, () -> {
            Map<String, Object> output = Map.of(
                    "requiresApproval", true,
                    "resource", "monster-config",
                    "action", "update",
                    "riskLevel", "L3_PROJECT_WRITE",
                    "quorum", 2,
                    "impactSummary", "5 files affected"
            );

            gate.validate(output, Map.class);

            verify(approvalGateway).create(any(ApprovalRequest.class));
            verify(notifier).notifyApprovalRequested(any(), anyList());
        });
    }
}
