package ai.gameclaw.governance.approval;

import ai.gameclaw.security.RiskLevel;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

public record ApprovalRequest(
        UUID requesterId,
        String resource,
        String action,
        RiskLevel riskLevel,
        String impactSummary,
        Map<String, Object> params,
        int quorum,
        Duration ttl
) {}
