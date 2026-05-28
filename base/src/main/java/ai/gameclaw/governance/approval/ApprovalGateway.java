package ai.gameclaw.governance.approval;

import ai.gameclaw.observability.AuditLogger;
import ai.gameclaw.security.RiskLevel;
import ai.gameclaw.security.Role;
import ai.gameclaw.security.TenantContext;
import ai.gameclaw.security.TenantContextHolder;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "gameclaw.governance.approval.enabled", havingValue = "true", matchIfMissing = true)
public class ApprovalGateway {

    private static final Logger log = LoggerFactory.getLogger(ApprovalGateway.class);

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<AuditLogger> auditLoggerProvider;

    public ApprovalGateway(JdbcTemplate jdbc, ObjectProvider<ObjectMapper> objectMapperProvider, ObjectProvider<AuditLogger> auditLoggerProvider) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapperProvider.getIfAvailable(ObjectMapper::new);
        this.auditLoggerProvider = auditLoggerProvider;
    }

    public PendingApproval create(ApprovalRequest request) {
        UUID id = UUID.randomUUID();
        TenantContext ctx = TenantContextHolder.tryGet().orElse(null);
        UUID tenantId = ctx != null ? ctx.tenantId() : null;
        Instant now = Instant.now();
        Instant expiresAt = now.plus(request.ttl());

        jdbc.update(
                "INSERT INTO pending_approvals (id, tenant_id, requester_id, resource, action, risk_level, impact_summary, params, quorum, approvals, state, expires_at, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?::jsonb, ?, ?, ?)",
                id, tenantId, request.requesterId(), request.resource(), request.action(),
                request.riskLevel().name(), request.impactSummary(),
                toJson(request.params()), request.quorum(),
                toJson(List.of()), ApprovalState.PENDING.name(),
                Timestamp.from(expiresAt), Timestamp.from(now)
        );

        writeAuditLog(tenantId, request.requesterId(), "approval_requested", request.resource(),
                Map.of("approvalId", id.toString(), "riskLevel", request.riskLevel().name(), "quorum", request.quorum()));

        return new PendingApproval(id, tenantId, request.requesterId(), request.resource(), request.action(),
                request.riskLevel(), request.impactSummary(), request.params(), request.quorum(),
                List.of(), ApprovalState.PENDING, expiresAt, now);
    }

    public Optional<PendingApproval> findById(UUID id) {
        List<PendingApproval> results = jdbc.query(
                "SELECT * FROM pending_approvals WHERE id = ?",
                (rs, rowNum) -> mapRow(rs), id);
        return results.stream().findFirst();
    }

    public List<PendingApproval> findPending(UUID tenantId) {
        return jdbc.query(
                "SELECT * FROM pending_approvals WHERE tenant_id = ? AND state = ?",
                (rs, rowNum) -> mapRow(rs), tenantId, ApprovalState.PENDING.name());
    }

    public PendingApproval approve(UUID approvalId, UUID userId, String reason) {
        PendingApproval approval = findById(approvalId)
                .orElseThrow(() -> new IllegalArgumentException("Approval not found: " + approvalId));

        if (approval.state() != ApprovalState.PENDING) {
            throw new IllegalStateException("Approval is not in PENDING state: " + approval.state());
        }

        if (approval.expiresAt().isBefore(Instant.now())) {
            updateState(approvalId, ApprovalState.EXPIRED);
            writeAuditLog(approval.tenantId(), userId, "approval_expired", approval.resource(),
                    Map.of("approvalId", approvalId.toString()));
            throw new IllegalStateException("Approval has expired: " + approvalId);
        }

        ApprovalEntry entry = new ApprovalEntry(userId, "approve", Instant.now(), reason);
        List<ApprovalEntry> updatedApprovals = new ArrayList<>(approval.approvals());
        updatedApprovals.add(entry);

        ApprovalState newState = ApprovalState.PENDING;
        long approveCount = updatedApprovals.stream()
                .filter(e -> "approve".equals(e.decision()) || "emergency_override".equals(e.decision()))
                .count();
        if (approveCount >= approval.quorum()) {
            newState = ApprovalState.APPROVED;
        }

        jdbc.update(
                "UPDATE pending_approvals SET approvals = ?::jsonb, state = ? WHERE id = ?",
                toJson(updatedApprovals), newState.name(), approvalId);

        writeAuditLog(approval.tenantId(), userId, "approval_approved", approval.resource(),
                Map.of("approvalId", approvalId.toString(), "newState", newState.name(), "reason", reason != null ? reason : ""));

        return new PendingApproval(approval.id(), approval.tenantId(), approval.requesterId(),
                approval.resource(), approval.action(), approval.riskLevel(), approval.impactSummary(),
                approval.params(), approval.quorum(), updatedApprovals, newState,
                approval.expiresAt(), approval.createdAt());
    }

    public PendingApproval reject(UUID approvalId, UUID userId, String reason) {
        PendingApproval approval = findById(approvalId)
                .orElseThrow(() -> new IllegalArgumentException("Approval not found: " + approvalId));

        if (approval.state() != ApprovalState.PENDING) {
            throw new IllegalStateException("Approval is not in PENDING state: " + approval.state());
        }

        ApprovalEntry entry = new ApprovalEntry(userId, "reject", Instant.now(), reason);
        List<ApprovalEntry> updatedApprovals = new ArrayList<>(approval.approvals());
        updatedApprovals.add(entry);

        jdbc.update(
                "UPDATE pending_approvals SET approvals = ?::jsonb, state = ? WHERE id = ?",
                toJson(updatedApprovals), ApprovalState.REJECTED.name(), approvalId);

        writeAuditLog(approval.tenantId(), userId, "approval_rejected", approval.resource(),
                Map.of("approvalId", approvalId.toString(), "reason", reason != null ? reason : ""));

        return new PendingApproval(approval.id(), approval.tenantId(), approval.requesterId(),
                approval.resource(), approval.action(), approval.riskLevel(), approval.impactSummary(),
                approval.params(), approval.quorum(), updatedApprovals, ApprovalState.REJECTED,
                approval.expiresAt(), approval.createdAt());
    }

    public int expireOverdue() {
        int updated = jdbc.update(
                "UPDATE pending_approvals SET state = ? WHERE state = ? AND expires_at < now()",
                ApprovalState.EXPIRED.name(), ApprovalState.PENDING.name());
        if (updated > 0) {
            log.info("[ApprovalGateway] Expired {} overdue approvals", updated);
        }
        return updated;
    }

    public PendingApproval emergencyOverride(UUID approvalId, UUID adminId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Emergency override requires a non-empty reason");
        }

        TenantContext ctx = TenantContextHolder.tryGet().orElse(null);
        if (ctx == null || ctx.roles() == null ||
                !(ctx.roles().contains(Role.ADMIN) || ctx.roles().contains(Role.PLATFORM_ADMIN))) {
            throw new SecurityException("Emergency override requires ADMIN or PLATFORM_ADMIN role");
        }

        PendingApproval approval = findById(approvalId)
                .orElseThrow(() -> new IllegalArgumentException("Approval not found: " + approvalId));

        if (approval.state() != ApprovalState.PENDING) {
            throw new IllegalStateException("Approval is not in PENDING state: " + approval.state());
        }

        ApprovalEntry entry = new ApprovalEntry(adminId, "emergency_override", Instant.now(), reason);
        List<ApprovalEntry> updatedApprovals = new ArrayList<>(approval.approvals());
        updatedApprovals.add(entry);

        jdbc.update(
                "UPDATE pending_approvals SET approvals = ?::jsonb, state = ? WHERE id = ?",
                toJson(updatedApprovals), ApprovalState.APPROVED.name(), approvalId);

        writeAuditLog(approval.tenantId(), adminId, "emergency_override", approval.resource(),
                Map.of("approvalId", approvalId.toString(), "emergency_override", true, "reason", reason));

        return new PendingApproval(approval.id(), approval.tenantId(), approval.requesterId(),
                approval.resource(), approval.action(), approval.riskLevel(), approval.impactSummary(),
                approval.params(), approval.quorum(), updatedApprovals, ApprovalState.APPROVED,
                approval.expiresAt(), approval.createdAt());
    }

    public List<PendingApproval> findByResource(UUID tenantId, String resource) {
        return jdbc.query(
                "SELECT * FROM pending_approvals WHERE tenant_id = ? AND resource = ?",
                (rs, rowNum) -> mapRow(rs), tenantId, resource);
    }

    private PendingApproval mapRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new PendingApproval(
                rs.getObject("id", UUID.class),
                rs.getObject("tenant_id", UUID.class),
                rs.getObject("requester_id", UUID.class),
                rs.getString("resource"),
                rs.getString("action"),
                RiskLevel.valueOf(rs.getString("risk_level")),
                rs.getString("impact_summary"),
                fromJson(rs.getString("params"), new TypeReference<Map<String, Object>>() {}),
                rs.getInt("quorum"),
                fromJson(rs.getString("approvals"), new TypeReference<List<ApprovalEntry>>() {}),
                ApprovalState.valueOf(rs.getString("state")),
                rs.getTimestamp("expires_at").toInstant(),
                rs.getTimestamp("created_at").toInstant()
        );
    }

    private void updateState(UUID approvalId, ApprovalState state) {
        jdbc.update("UPDATE pending_approvals SET state = ? WHERE id = ?", state.name(), approvalId);
    }

    private void writeAuditLog(UUID tenantId, UUID actorId, String action, String resource, Map<String, Object> detail) {
        try {
            jdbc.update(
                    "INSERT INTO audit_log (id, tenant_id, actor_id, action, resource, detail, created_at) " +
                            "VALUES (gen_random_uuid(), ?, ?, ?, ?, ?::jsonb, now())",
                    tenantId, actorId, action, resource, toJson(detail));
        } catch (Exception e) {
            log.warn("[ApprovalGateway] Failed to write audit_log: {}", e.getMessage());
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize JSON", e);
        }
    }

    private <T> T fromJson(String value, TypeReference<T> typeRef) {
        try {
            if (value == null || value.isBlank()) {
                return null;
            }
            return objectMapper.readValue(value, typeRef);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize JSON", e);
        }
    }
}
