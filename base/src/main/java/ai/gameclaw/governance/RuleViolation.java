package ai.gameclaw.governance;

public record RuleViolation(String ruleName, String field, String message) {}
