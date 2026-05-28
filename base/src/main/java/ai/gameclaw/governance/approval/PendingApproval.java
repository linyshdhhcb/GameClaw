package ai.gameclaw.governance.approval;

import ai.gameclaw.security.RiskLevel;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record PendingApproval(
        UUID id,
        UUID tenantId,
        UUID requesterId,
        String resource,
        String action,
        RiskLevel riskLevel,
        String impactSummary,
        Map<String, Object> params,
        int quorum,
        List<ApprovalEntry> approvals,
        ApprovalState state,
        Instant expiresAt,
        Instant createdAt
) {}
