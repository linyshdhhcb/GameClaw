package ai.gameclaw.agent.llm;

public record ChatResponse(
        String content,
        String model,
        long usagePromptTokens,
        long usageCompletionTokens
) {
    public ChatResponse(String content) {
        this(content, null, 0, 0);
    }
}
