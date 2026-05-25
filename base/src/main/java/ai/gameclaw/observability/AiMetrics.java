package ai.gameclaw.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class AiMetrics {

    private final MeterRegistry registry;

    private final Counter llmRequestTotal;
    private final Counter llmTokensInTotal;
    private final Counter llmTokensOutTotal;
    private final Counter llmCostCnyTotal;
    private final Counter toolCallTotal;
    private final Counter validationGateFailureTotal;
    private final Counter quotaExhaustedTotal;

    public AiMetrics(MeterRegistry registry) {
        this.registry = registry;
        this.llmRequestTotal = Counter.builder("llm_request_total")
                .description("Total LLM API requests")
                .register(registry);
        this.llmTokensInTotal = Counter.builder("llm_tokens_in_total")
                .description("Total LLM input tokens consumed")
                .register(registry);
        this.llmTokensOutTotal = Counter.builder("llm_tokens_out_total")
                .description("Total LLM output tokens generated")
                .register(registry);
        this.llmCostCnyTotal = Counter.builder("llm_cost_cny_total")
                .description("Total LLM cost in CNY")
                .register(registry);
        this.toolCallTotal = Counter.builder("tool_call_total")
                .description("Total tool invocations")
                .register(registry);
        this.validationGateFailureTotal = Counter.builder("validation_gate_failure_total")
                .description("Total validation gate failures")
                .register(registry);
        this.quotaExhaustedTotal = Counter.builder("quota_exhausted_total")
                .description("Total quota exhaustion events")
                .register(registry);
    }

    public void recordLlmRequest(String model, String role) {
        Counter.builder("llm_request_total")
                .tag("model", model)
                .tag("role", role)
                .register(registry).increment();
    }

    public void recordTokens(String model, int promptTokens, int completionTokens) {
        Counter.builder("llm_tokens_in_total")
                .tag("model", model)
                .register(registry).increment(promptTokens);
        Counter.builder("llm_tokens_out_total")
                .tag("model", model)
                .register(registry).increment(completionTokens);
    }

    public void recordCost(String model, String project, double costCny) {
        Counter.builder("llm_cost_cny_total")
                .tag("model", model)
                .tag("project", project)
                .register(registry).increment(costCny);
    }

    public Timer.Sample startLlmLatency() {
        return Timer.start(registry);
    }

    public void recordLlmLatency(String model, Timer.Sample sample, long durationNanos) {
        sample.stop(Timer.builder("llm_latency_seconds")
                .tag("model", model)
                .publishPercentileHistogram()
                .sla(Duration.ofSeconds(1), Duration.ofSeconds(5), Duration.ofSeconds(10), Duration.ofSeconds(30))
                .register(registry));
    }

    public void recordToolCall(String tool, String result) {
        Counter.builder("tool_call_total")
                .tag("tool", tool)
                .tag("result", result)
                .register(registry).increment();
    }

    public void recordValidationGateFailure(String gate, String tool) {
        Counter.builder("validation_gate_failure_total")
                .tag("gate", gate)
                .tag("tool", tool)
                .register(registry).increment();
    }

    public void recordQuotaExhausted(String project) {
        Counter.builder("quota_exhausted_total")
                .tag("project", project)
                .register(registry).increment();
    }
}
