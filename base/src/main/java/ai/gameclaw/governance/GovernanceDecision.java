package ai.gameclaw.governance;

public record GovernanceDecision(
        boolean allowed,
        String reason
) {
    public static GovernanceDecision allow() {
        return new GovernanceDecision(true, "");
    }

    public static GovernanceDecision deny(String reason) {
        return new GovernanceDecision(false, reason);
    }
}
