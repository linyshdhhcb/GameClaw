package ai.gameclaw.governance;

import ai.gameclaw.observability.AiMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.validation.Validation;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidatedLlmOutputTest {

    private MeterRegistry registry;
    private AiMetrics aiMetrics;
    private ValidationGate1Schema schemaGate;

    record SampleOutput(@Min(1) int id, @NotBlank String name) {}

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        aiMetrics = new AiMetrics(registry);
        schemaGate = new ValidationGate1Schema(
                new com.fasterxml.jackson.databind.ObjectMapper(),
                Validation.buildDefaultValidatorFactory().getValidator()
        );
    }

    @Test
    void validOnFirstAttempt() {
        var validInput = java.util.Map.of("id", 1, "name", "test");
        ValidatedLlmOutput<SampleOutput> result = ValidatedLlmOutput.validate(
                () -> validInput,
                SampleOutput.class,
                List.of(schemaGate),
                aiMetrics,
                "test-tool"
        );
        assertThat(result.result().id()).isEqualTo(1);
        assertThat(result.result().name()).isEqualTo("test");
        assertThat(result.attempts()).isEqualTo(1);
    }

    @Test
    void retryOnFailureThenSucceed() {
        var invalidInput = java.util.Map.of("id", 0, "name", "");
        var validInput = java.util.Map.of("id", 1, "name", "test");
        var inputs = List.of(invalidInput, validInput);
        var index = new int[]{0};

        ValidatedLlmOutput<SampleOutput> result = ValidatedLlmOutput.validate(
                () -> inputs.get(Math.min(index[0]++, inputs.size() - 1)),
                SampleOutput.class,
                List.of(schemaGate),
                aiMetrics,
                "test-tool",
                2
        );
        assertThat(result.result().id()).isEqualTo(1);
        assertThat(result.attempts()).isEqualTo(2);
        assertThat(registry.counter("validation_gate_failure_total", "gate", "schema", "tool", "test-tool").count()).isGreaterThanOrEqualTo(1.0);
    }

    @Test
    void allRetriesExhausted() {
        var invalidInput = java.util.Map.of("id", 0, "name", "");
        ValidatedLlmOutput<SampleOutput> output = ValidatedLlmOutput.validate(
                () -> invalidInput,
                SampleOutput.class,
                List.of(schemaGate),
                aiMetrics,
                "test-tool",
                2
        );
        assertThatThrownBy(output::result)
                .isInstanceOf(ValidationGateException.class)
                .satisfies(ex -> {
                    ValidationGateException vge = (ValidationGateException) ex;
                    assertThat(vge.attempts()).isEqualTo(3);
                });
    }
}
