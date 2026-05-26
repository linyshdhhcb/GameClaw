package ai.gameclaw.agent.llm;

public record ModelChoice(String modelId, String fallback, Complexity complexity) {
}
