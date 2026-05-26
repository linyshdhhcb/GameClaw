package ai.gameclaw.agent.llm;

import ai.gameclaw.observability.AiMetrics;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FallbackChainTest {

    private CircuitBreakerRegistry circuitBreakerRegistry;

    @BeforeEach
    void setUp() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .slidingWindowSize(50)
                .build();
        circuitBreakerRegistry = CircuitBreakerRegistry.of(config);
        circuitBreakerRegistry.circuitBreaker("llm-default", config);
    }

    @Test
    void primarySucceeds_returnsResult() {
        LlmClient primary = mock(LlmClient.class);
        ChatResponse expected = new ChatResponse("hello");
        when(primary.call(any())).thenReturn(expected);

        FallbackChain chain = new FallbackChain(
                primary,
                stubProviders(List.of()),
                circuitBreakerRegistry,
                stubAiMetrics()
        );

        ChatResponse result = chain.callWithFallback(new ChatRequest("test"));
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void primaryFails_fallbackToSecondary_succeeds() {
        LlmClient primary = mock(LlmClient.class);
        LlmClient secondary = mock(LlmClient.class);
        ChatResponse expected = new ChatResponse("fallback response");

        when(primary.call(any())).thenThrow(new RuntimeException("primary down"));
        when(secondary.call(any())).thenReturn(expected);

        FallbackChain chain = new FallbackChain(
                primary,
                stubProviders(List.of(secondary)),
                circuitBreakerRegistry,
                stubAiMetrics()
        );

        ChatResponse result = chain.callWithFallback(new ChatRequest("test"));
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void allFail_throwsRuntimeException() {
        LlmClient primary = mock(LlmClient.class);
        when(primary.call(any())).thenThrow(new RuntimeException("primary down"));

        FallbackChain chain = new FallbackChain(
                primary,
                stubProviders(List.of()),
                circuitBreakerRegistry,
                stubAiMetrics()
        );

        assertThatThrownBy(() -> chain.callWithFallback(new ChatRequest("test")))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("All LLM providers failed")
                .hasCauseInstanceOf(RuntimeException.class);
    }

    @Test
    void circuitBreakerTracksFailures() {
        LlmClient primary = mock(LlmClient.class);
        when(primary.call(any())).thenThrow(new RuntimeException("fail"));

        FallbackChain chain = new FallbackChain(
                primary,
                stubProviders(List.of()),
                circuitBreakerRegistry,
                stubAiMetrics()
        );

        for (int i = 0; i < 10; i++) {
            try {
                chain.callWithFallback(new ChatRequest("test"));
            } catch (RuntimeException ignored) {
            }
        }

        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("llm-default");
        assertThat(cb.getMetrics().getNumberOfFailedCalls()).isGreaterThan(0);
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<List<LlmClient>> stubProviders(List<LlmClient> providers) {
        ObjectProvider<List<LlmClient>> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable(any())).thenReturn(providers);
        return provider;
    }

    private ObjectProvider<AiMetrics> stubAiMetrics() {
        ObjectProvider<AiMetrics> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(new AiMetrics(new SimpleMeterRegistry()));
        return provider;
    }
}
