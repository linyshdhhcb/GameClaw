package ai.gameclaw.governance;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ValidationGate2RulesTest {

    private ObjectMapper objectMapper;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    void noRules_outputPasses() {
        ValidationGate2Rules gate = new ValidationGate2Rules(List.of());
        Map<String, Object> monster = Map.of(
                "name", "Goblin", "hp", 100, "attack", 50, "level", 5, "dropRate", 0.5
        );
        ValidationResult result = gate.validate(monster, Map.class);
        assertThat(result.valid()).isTrue();
        assertThat(result.parsed()).isSameAs(monster);
    }

    private List<BusinessRule> createDefaultRules() {
        DefaultBusinessRules dbr = new DefaultBusinessRules(objectMapper);
        return List.of(
                dbr.monsterHpRangeRule(),
                dbr.monsterAttackRangeRule(),
                dbr.monsterLevelRangeRule(),
                dbr.monsterDropRateRangeRule(),
                dbr.skillDamageRangeRule(),
                dbr.skillCooldownRangeRule(),
                dbr.itemPriceRangeRule(),
                dbr.questRewardGoldRangeRule(),
                dbr.growthCurvePointValueRangeRule(),
                dbr.noNullOrEmptyNameRule()
        );
    }

    @Test
    void monsterHpExceedsMax_violationDetected() {
        List<BusinessRule> rules = createDefaultRules();
        ValidationGate2Rules gate = new ValidationGate2Rules(rules);

        Map<String, Object> monster = Map.of(
                "name", "Boss", "hp", 99_999_999, "attack", 50, "level", 5, "dropRate", 0.5
        );
        ValidationResult result = gate.validate(monster, Map.class);
        assertThat(result.valid()).isFalse();
        assertThat(result.code()).isEqualTo("RULES_VIOLATION");
        assertThat(result.violations()).anyMatch(v -> v.contains("monster-hp-range"));
    }

    @Test
    void monsterWithValidValues_passes() {
        List<BusinessRule> rules = createDefaultRules();
        ValidationGate2Rules gate = new ValidationGate2Rules(rules);

        Map<String, Object> monster = Map.of(
                "name", "Goblin", "hp", 100, "attack", 50, "level", 5, "dropRate", 0.5
        );
        ValidationResult result = gate.validate(monster, Map.class);
        assertThat(result.valid()).isTrue();
    }

    @Test
    void multipleViolationsInOneObject() {
        List<BusinessRule> rules = createDefaultRules();
        ValidationGate2Rules gate = new ValidationGate2Rules(rules);

        Map<String, Object> monster = Map.of(
                "name", "Broken", "hp", 99_999_999, "attack", 999_999, "level", 999, "dropRate", 5.0
        );
        ValidationResult result = gate.validate(monster, Map.class);
        assertThat(result.valid()).isFalse();
        assertThat(result.violations()).hasSizeGreaterThanOrEqualTo(4);
        assertThat(result.violations()).anyMatch(v -> v.contains("monster-hp-range"));
        assertThat(result.violations()).anyMatch(v -> v.contains("monster-attack-range"));
        assertThat(result.violations()).anyMatch(v -> v.contains("monster-level-range"));
        assertThat(result.violations()).anyMatch(v -> v.contains("monster-drop-rate-range"));
    }

    @Test
    void listOfConfigs_eachValidated() {
        List<BusinessRule> rules = createDefaultRules();
        ValidationGate2Rules gate = new ValidationGate2Rules(rules);

        Map<String, Object> validMonster = Map.of(
                "name", "Goblin", "hp", 100, "attack", 50, "level", 5, "dropRate", 0.5
        );
        Map<String, Object> invalidMonster = Map.of(
                "name", "Boss", "hp", 99_999_999, "attack", 50, "level", 5, "dropRate", 0.5
        );
        List<Map<String, Object>> monsters = List.of(validMonster, invalidMonster);
        ValidationResult result = gate.validate(monsters, List.class);
        assertThat(result.valid()).isFalse();
        assertThat(result.violations()).anyMatch(v -> v.contains("monster-hp-range"));
    }

    @Test
    void customRuleFromYaml_loadedAndApplied() throws IOException {
        Path rulesDir = tempDir.resolve("game-skills").resolve("validation-rules");
        Files.createDirectories(rulesDir);

        String yaml = """
                name: "custom-hp-range"
                description: "HP must be between 10 and 500000"
                field: "hp"
                min: 10
                max: 500000
                appliesTo: "MonsterConfig"
                """;
        Files.writeString(rulesDir.resolve("custom-rule.yaml"), yaml);

        CustomRuleLoader loader = new CustomRuleLoader(objectMapper, "file:" + tempDir);
        loader.loadRules();

        List<BusinessRule> allRules = new java.util.ArrayList<>(loader.getCustomRules());
        ValidationGate2Rules gate = new ValidationGate2Rules(allRules);

        Map<String, Object> monster = Map.of(
                "name", "Boss", "hp", 600_000, "attack", 50, "level", 5, "dropRate", 0.5
        );
        ValidationResult result = gate.validate(monster, Map.class);
        assertThat(result.valid()).isFalse();
        assertThat(result.violations()).anyMatch(v -> v.contains("custom-hp-range"));
    }

    @Test
    void customRuleFromYaml_validValue_passes() throws IOException {
        Path rulesDir = tempDir.resolve("game-skills").resolve("validation-rules");
        Files.createDirectories(rulesDir);

        String yaml = """
                name: "custom-hp-range"
                description: "HP must be between 10 and 500000"
                field: "hp"
                min: 10
                max: 500000
                appliesTo: "MonsterConfig"
                """;
        Files.writeString(rulesDir.resolve("custom-rule.yaml"), yaml);

        CustomRuleLoader loader = new CustomRuleLoader(objectMapper, "file:" + tempDir);
        loader.loadRules();

        List<BusinessRule> allRules = new java.util.ArrayList<>(loader.getCustomRules());
        ValidationGate2Rules gate = new ValidationGate2Rules(allRules);

        Map<String, Object> monster = Map.of(
                "name", "Goblin", "hp", 100, "attack", 50, "level", 5, "dropRate", 0.5
        );
        ValidationResult result = gate.validate(monster, Map.class);
        assertThat(result.valid()).isTrue();
    }

    @Test
    void gateName() {
        ValidationGate2Rules gate = new ValidationGate2Rules(List.of());
        assertThat(gate.name()).isEqualTo("rules");
    }
}
