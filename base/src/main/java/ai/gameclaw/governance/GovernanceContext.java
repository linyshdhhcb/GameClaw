package ai.gameclaw.governance;

import java.util.Map;

public record GovernanceContext(
        String agentId,
        String action,
        Map<String, Object> metadata
) {
}
