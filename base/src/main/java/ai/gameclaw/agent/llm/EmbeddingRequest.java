package ai.gameclaw.agent.llm;

public record EmbeddingRequest(String text, String model) {
    public EmbeddingRequest(String text) {
        this(text, null);
    }
}
