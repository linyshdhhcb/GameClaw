package ai.gameclaw.governance;

import ai.gameclaw.observability.AiMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Supplier;

public class ValidatedLlmOutput<T> {

    private static final Logger log = LoggerFactory.getLogger(ValidatedLlmOutput.class);

    private static final int DEFAULT_MAX_RETRIES = 2;

    private final T result;
    private final ValidationResult lastValidation;
    private final int attempts;

    private ValidatedLlmOutput(T result, ValidationResult lastValidation, int attempts) {
        this.result = result;
        this.lastValidation = lastValidation;
        this.attempts = attempts;
    }

    public T result() {
        if (result == null) {
            throw new ValidationGateException(
                    lastValidation.code(),
                    lastValidation.message(),
                    lastValidation.violations(),
                    attempts
            );
        }
        return result;
    }

    public ValidationResult lastValidation() {
        return lastValidation;
    }

    public int attempts() {
        return attempts;
    }

    public static <T> ValidatedLlmOutput<T> validate(
            Supplier<Object> llmCall,
            Class<T> expectedType,
            List<ValidationGate> gates,
            AiMetrics aiMetrics,
            String toolName) {
        return validate(llmCall, expectedType, gates, aiMetrics, toolName, DEFAULT_MAX_RETRIES);
    }

    public static <T> ValidatedLlmOutput<T> validate(
            Supplier<Object> llmCall,
            Class<T> expectedType,
            List<ValidationGate> gates,
            AiMetrics aiMetrics,
            String toolName,
            int maxRetries) {

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            Object rawOutput = llmCall.get();
            boolean allGatesPassed = true;

            for (ValidationGate gate : gates) {
                ValidationResult vr = gate.validate(rawOutput, expectedType);
                if (!vr.valid()) {
                    allGatesPassed = false;
                    log.warn("[ValidatedLlmOutput] Gate '{}' rejected output (attempt {}/{}): {} - {}",
                            gate.name(), attempt + 1, maxRetries + 1, vr.code(), vr.message());
                    if (aiMetrics != null) {
                        aiMetrics.recordValidationGateFailure(gate.name(), toolName);
                    }
                    if (attempt >= maxRetries) {
                        return new ValidatedLlmOutput<>(null, vr, attempt + 1);
                    }
                    break;
                }
                rawOutput = vr.parsed();
            }

            if (allGatesPassed) {
                @SuppressWarnings("unchecked")
                T typed = (T) rawOutput;
                return new ValidatedLlmOutput<>(typed, ValidationResult.ok(typed), attempt + 1);
            }
        }

        return new ValidatedLlmOutput<>(null,
                ValidationResult.fail("MAX_RETRIES_EXCEEDED", "Exceeded max retries: " + maxRetries),
                maxRetries + 1);
    }
}
