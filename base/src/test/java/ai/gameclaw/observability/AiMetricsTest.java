package ai.gameclaw.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiMetricsTest {

    private MeterRegistry registry;
    private AiMetrics aiMetrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        aiMetrics = new AiMetrics(registry);
    }

    @Test
    void recordLlmRequest() {
        aiMetrics.recordLlmRequest("gpt-4", "assistant");
        assertThat(registry.counter("llm_request_total", "model", "gpt-4", "role", "assistant").count()).isEqualTo(1.0);
    }

    @Test
    void recordTokens() {
        aiMetrics.recordTokens("gpt-4", 100, 50);
        assertThat(registry.counter("llm_tokens_in_total", "model", "gpt-4").count()).isEqualTo(100.0);
        assertThat(registry.counter("llm_tokens_out_total", "model", "gpt-4").count()).isEqualTo(50.0);
    }

    @Test
    void recordCost() {
        aiMetrics.recordCost("gpt-4", "project-a", 0.05);
        assertThat(registry.counter("llm_cost_cny_total", "model", "gpt-4", "project", "project-a").count()).isCloseTo(0.05, org.assertj.core.data.Offset.offset(0.001));
    }

    @Test
    void recordToolCall() {
        aiMetrics.recordToolCall("generate_monster", "success");
        assertThat(registry.counter("tool_call_total", "tool", "generate_monster", "result", "success").count()).isEqualTo(1.0);
    }

    @Test
    void recordValidationGateFailure() {
        aiMetrics.recordValidationGateFailure("schema", "generate_monster");
        assertThat(registry.counter("validation_gate_failure_total", "gate", "schema", "tool", "generate_monster").count()).isEqualTo(1.0);
    }

    @Test
    void recordQuotaExhausted() {
        aiMetrics.recordQuotaExhausted("project-a");
        assertThat(registry.counter("quota_exhausted_total", "project", "project-a").count()).isEqualTo(1.0);
    }
}
