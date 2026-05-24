package ai.gameclaw.governance;

public interface GovernancePolicy {

    String getName();

    boolean evaluate(GovernanceContext context);

    GovernanceDecision decide(GovernanceContext context);
}
