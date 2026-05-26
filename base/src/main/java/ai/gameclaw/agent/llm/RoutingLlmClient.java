package ai.gameclaw.agent.llm;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class RoutingLlmClient implements LlmClient {

    private final LlmClient delegate;
    private final ModelRouter modelRouter;
    private final FallbackChain fallbackChain;

    public RoutingLlmClient(LlmClient delegate, ModelRouter modelRouter, FallbackChain fallbackChain) {
        this.delegate = delegate;
        this.modelRouter = modelRouter;
        this.fallbackChain = fallbackChain;
    }

    @Override
    public ChatResponse call(ChatRequest request) {
        ModelChoice choice = modelRouter.route(request);
        return fallbackChain.callWithFallback(request);
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
