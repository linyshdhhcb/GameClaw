package ai.gameclaw.governance;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Order(1)
@ConditionalOnBean(ObjectMapper.class)
public class ValidationGate1Schema implements ValidationGate {

    private static final Logger log = LoggerFactory.getLogger(ValidationGate1Schema.class);

    private final ObjectMapper objectMapper;
    private final Validator validator;

    public ValidationGate1Schema(ObjectMapper objectMapper, Validator validator) {
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    @Override
    public String name() {
        return "schema";
    }

    @Override
    public ValidationResult validate(Object output, Class<?> expectedType) {
        Object parsed;
        try {
            parsed = objectMapper.convertValue(output, expectedType);
        } catch (IllegalArgumentException e) {
            log.debug("[Gate1:Schema] Deserialization failed: {}", e.getMessage());
            return ValidationResult.fail("SCHEMA_DESERIALIZE_ERROR",
                    "Failed to deserialize to " + expectedType.getSimpleName() + ": " + e.getMessage());
        }

        Set<ConstraintViolation<Object>> violations = validator.validate(parsed);
        if (!violations.isEmpty()) {
            List<String> violationMessages = violations.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage())
                    .collect(Collectors.toList());
            log.debug("[Gate1:Schema] Bean validation failed: {}", violationMessages);
            return ValidationResult.fail("SCHEMA_VALIDATION_ERROR",
                    "Bean validation failed for " + expectedType.getSimpleName(),
                    violationMessages);
        }

        return ValidationResult.ok(parsed);
    }
}
