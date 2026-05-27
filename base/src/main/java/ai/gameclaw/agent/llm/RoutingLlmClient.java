package ai.gameclaw.agent.llm;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class RoutingLlmClient implements LlmClient {

    private final LlmClient delegate;
    private final ModelRouter modelRouter;

    public RoutingLlmClient(@Qualifier("springAiLlmClient") LlmClient delegate, ModelRouter modelRouter) {
        this.delegate = delegate;
        this.modelRouter = modelRouter;
    }

    @Override
    public ChatResponse call(ChatRequest request) {
        ModelChoice choice = modelRouter.route(request);
        return delegate.call(request);
    }

    @Override
    public Flux<ChatResponseChunk> stream(ChatRequest request) {
        return delegate.stream(request);
    }

    @Override
    public EmbeddingResponse embed(EmbeddingRequest request) {
        return delegate.embed(request);
    }
}
