package ai.gameclaw.agent.llm;

public interface LlmClient {

    ChatResponse call(ChatRequest request);

    reactor.core.publisher.Flux<ChatResponseChunk> stream(ChatRequest request);

    EmbeddingResponse embed(EmbeddingRequest request);
}
