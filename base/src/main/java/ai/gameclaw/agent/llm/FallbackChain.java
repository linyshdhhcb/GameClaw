package ai.gameclaw.agent.llm;

import ai.gameclaw.observability.AiMetrics;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class FallbackChain {

    private static final Logger log = LoggerFactory.getLogger(FallbackChain.class);

    private static final String CIRCUIT_BREAKER_NAME = "llm-default";

    private final LlmClient primary;
    private final List<LlmClient> providers;
    private final CircuitBreaker circuitBreaker;
    private final ObjectProvider<AiMetrics> aiMetricsProvider;

    public FallbackChain(LlmClient primary,
                         ObjectProvider<List<LlmClient>> providersProvider,
                         CircuitBreakerRegistry circuitBreakerRegistry,
                         ObjectProvider<AiMetrics> aiMetricsProvider) {
        this.primary = primary;
        List<LlmClient> all = providersProvider.getIfAvailable(List::of);
        this.providers = all.stream()
                .filter(c -> c != primary)
                .toList();
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(CIRCUIT_BREAKER_NAME);
        this.aiMetricsProvider = aiMetricsProvider;
    }

    public ChatResponse callWithFallback(ChatRequest request) {
        List<LlmClient> chain = buildChain();
        Exception lastError = null;

        for (int i = 0; i < chain.size(); i++) {
            LlmClient client = chain.get(i);
            try {
                ChatResponse response = circuitBreaker.executeSupplier(() -> client.call(request));
                return response;
            } catch (Exception e) {
                lastError = e;
                log.warn("LlmClient {} failed, attempting fallback (attempt {}/{}): {}",
                        client.getClass().getSimpleName(), i + 1, chain.size(), e.getMessage());
                recordFallbackMetric();
            }
        }

        throw new RuntimeException("All LLM providers failed", lastError);
    }

    private List<LlmClient> buildChain() {
        List<LlmClient> chain = new ArrayList<>();
        chain.add(primary);
        chain.addAll(providers);
        return chain;
    }

    private void recordFallbackMetric() {
        AiMetrics metrics = aiMetricsProvider.getIfAvailable();
        if (metrics != null) {
            metrics.recordLlmRequest("fallback", "fallback");
        }
    }
}
