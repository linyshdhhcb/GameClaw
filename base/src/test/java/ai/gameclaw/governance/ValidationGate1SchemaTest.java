package ai.gameclaw.governance;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Path;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ValidationGate1SchemaTest {

    private ValidationGate1Schema gate;

    record MonsterConfig(
            @Min(1) int id,
            @NotBlank String name,
            @Min(1) @Max(1_000_000) int hp
    ) {}

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new ObjectMapper();
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        gate = new ValidationGate1Schema(mapper, validator);
    }

    @Test
    void name() {
        assertThat(gate.name()).isEqualTo("schema");
    }

    @Test
    void validOutput() {
        var input = java.util.Map.of("id", 1, "name", "Goblin", "hp", 100);
        ValidationResult result = gate.validate(input, MonsterConfig.class);
        assertThat(result.valid()).isTrue();
        assertThat(result.parsed()).isInstanceOf(MonsterConfig.class);
        MonsterConfig mc = (MonsterConfig) result.parsed();
        assertThat(mc.id()).isEqualTo(1);
        assertThat(mc.name()).isEqualTo("Goblin");
        assertThat(mc.hp()).isEqualTo(100);
    }

    @Test
    void invalidBeanValidation() {
        var input = java.util.Map.of("id", 0, "name", "", "hp", 0);
        ValidationResult result = gate.validate(input, MonsterConfig.class);
        assertThat(result.valid()).isFalse();
        assertThat(result.code()).isEqualTo("SCHEMA_VALIDATION_ERROR");
        assertThat(result.violations()).isNotEmpty();
    }

    @Test
    void deserializationFailure() {
        var input = "not-a-map";
        ValidationResult result = gate.validate(input, MonsterConfig.class);
        assertThat(result.valid()).isFalse();
        assertThat(result.code()).isEqualTo("SCHEMA_DESERIALIZE_ERROR");
    }
}
