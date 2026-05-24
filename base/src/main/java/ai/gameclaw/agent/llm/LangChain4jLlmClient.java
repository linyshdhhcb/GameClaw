package ai.gameclaw.agent.llm;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
@ConditionalOnProperty(name = "gameclaw.llm.adapter", havingValue = "langchain4j")
public class LangChain4jLlmClient implements LlmClient {

    @Override
    public ChatResponse call(ChatRequest request) {
        throw new UnsupportedOperationException("LangChain4j adapter is not yet implemented. Set gameclaw.llm.adapter=spring-ai to use the Spring AI adapter.");
    }

    @Override
    public Flux<ChatResponseChunk> stream(ChatRequest request) {
        return Flux.error(new UnsupportedOperationException("LangChain4j streaming is not yet implemented"));
    }

    @Override
    public EmbeddingResponse embed(EmbeddingRequest request) {
        throw new UnsupportedOperationException("LangChain4j embedding is not yet implemented");
    }
}
