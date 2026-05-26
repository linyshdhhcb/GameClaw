package ai.gameclaw.governance;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultBusinessRulesTest {

    private List<BusinessRule> rules;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        DefaultBusinessRules defaultBusinessRules = new DefaultBusinessRules(objectMapper);
        rules = defaultBusinessRules.defaultBusinessRules();
    }

    private BusinessRule findRule(String name) {
        return rules.stream()
                .filter(r -> r.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Rule not found: " + name));
    }

    @Test
    void tenDefaultRulesExist() {
        assertThat(rules).hasSize(10);
    }

    @Test
    void monsterHpRange_validValue_passes() {
        BusinessRule rule = findRule("monster-hp-range");
        Map<String, Object> monster = Map.of("name", "Goblin", "hp", 500, "attack", 50, "level", 5, "dropRate", 0.5);
        List<RuleViolation> violations = rule.validate(monster);
        assertThat(violations).isEmpty();
    }

    @Test
    void monsterHpRange_exceedsMax_violation() {
        BusinessRule rule = findRule("monster-hp-range");
        Map<String, Object> monster = Map.of("name", "Boss", "hp", 2_000_000, "attack", 50, "level", 5, "dropRate", 0.5);
        List<RuleViolation> violations = rule.validate(monster);
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).ruleName()).isEqualTo("monster-hp-range");
        assertThat(violations.get(0).field()).isEqualTo("hp");
    }

    @Test
    void monsterHpRange_belowMin_violation() {
        BusinessRule rule = findRule("monster-hp-range");
        Map<String, Object> monster = Map.of("name", "Bug", "hp", 0, "attack", 50, "level", 5, "dropRate", 0.5);
        List<RuleViolation> violations = rule.validate(monster);
        assertThat(violations).hasSize(1);
    }

    @Test
    void monsterAttackRange_validValue_passes() {
        BusinessRule rule = findRule("monster-attack-range");
        Map<String, Object> monster = Map.of("name", "Goblin", "hp", 500, "attack", 5000, "level", 5, "dropRate", 0.5);
        List<RuleViolation> violations = rule.validate(monster);
        assertThat(violations).isEmpty();
    }

    @Test
    void monsterAttackRange_exceedsMax_violation() {
        BusinessRule rule = findRule("monster-attack-range");
        Map<String, Object> monster = Map.of("name", "Boss", "hp", 500, "attack", 200_000, "level", 5, "dropRate", 0.5);
        List<RuleViolation> violations = rule.validate(monster);
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).field()).isEqualTo("attack");
    }

    @Test
    void monsterLevelRange_validValue_passes() {
        BusinessRule rule = findRule("monster-level-range");
        Map<String, Object> monster = Map.of("name", "Goblin", "hp", 500, "attack", 50, "level", 50, "dropRate", 0.5);
        List<RuleViolation> violations = rule.validate(monster);
        assertThat(violations).isEmpty();
    }

    @Test
    void monsterLevelRange_exceedsMax_violation() {
        BusinessRule rule = findRule("monster-level-range");
        Map<String, Object> monster = Map.of("name", "Boss", "hp", 500, "attack", 50, "level", 200, "dropRate", 0.5);
        List<RuleViolation> violations = rule.validate(monster);
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).field()).isEqualTo("level");
    }

    @Test
    void monsterDropRateRange_validValue_passes() {
        BusinessRule rule = findRule("monster-drop-rate-range");
        Map<String, Object> monster = Map.of("name", "Goblin", "hp", 500, "attack", 50, "level", 5, "dropRate", 0.75);
        List<RuleViolation> violations = rule.validate(monster);
        assertThat(violations).isEmpty();
    }

    @Test
    void monsterDropRateRange_exceedsMax_violation() {
        BusinessRule rule = findRule("monster-drop-rate-range");
        Map<String, Object> monster = Map.of("name", "Boss", "hp", 500, "attack", 50, "level", 5, "dropRate", 2.0);
        List<RuleViolation> violations = rule.validate(monster);
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).field()).isEqualTo("dropRate");
    }

    @Test
    void skillDamageRange_validValue_passes() {
        BusinessRule rule = findRule("skill-damage-range");
        Map<String, Object> skill = Map.of("name", "Fireball", "damage", 500, "cooldown", 10);
        List<RuleViolation> violations = rule.validate(skill);
        assertThat(violations).isEmpty();
    }

    @Test
    void skillDamageRange_exceedsMax_violation() {
        BusinessRule rule = findRule("skill-damage-range");
        Map<String, Object> skill = Map.of("name", "Nuke", "damage", 5_000_000, "cooldown", 10);
        List<RuleViolation> violations = rule.validate(skill);
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).field()).isEqualTo("damage");
    }

    @Test
    void skillCooldownRange_validValue_passes() {
        BusinessRule rule = findRule("skill-cooldown-range");
        Map<String, Object> skill = Map.of("name", "Fireball", "damage", 500, "cooldown", 30);
        List<RuleViolation> violations = rule.validate(skill);
        assertThat(violations).isEmpty();
    }

    @Test
    void skillCooldownRange_exceedsMax_violation() {
        BusinessRule rule = findRule("skill-cooldown-range");
        Map<String, Object> skill = Map.of("name", "SlowSpell", "damage", 500, "cooldown", 9999);
        List<RuleViolation> violations = rule.validate(skill);
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).field()).isEqualTo("cooldown");
    }

    @Test
    void itemPriceRange_validValue_passes() {
        BusinessRule rule = findRule("item-price-range");
        Map<String, Object> item = Map.of("name", "Sword", "rarity", "rare", "price", 5000);
        List<RuleViolation> violations = rule.validate(item);
        assertThat(violations).isEmpty();
    }

    @Test
    void itemPriceRange_exceedsMax_violation() {
        BusinessRule rule = findRule("item-price-range");
        Map<String, Object> item = Map.of("name", "Excalibur", "rarity", "legendary", "price", 50_000_000);
        List<RuleViolation> violations = rule.validate(item);
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).field()).isEqualTo("price");
    }

    @Test
    void questRewardGoldRange_validValue_passes() {
        BusinessRule rule = findRule("quest-reward-gold-range");
        Map<String, Object> quest = Map.of("name", "Fetch Quest", "objective", "Get item", "rewardGold", 1000);
        List<RuleViolation> violations = rule.validate(quest);
        assertThat(violations).isEmpty();
    }

    @Test
    void questRewardGoldRange_exceedsMax_violation() {
        BusinessRule rule = findRule("quest-reward-gold-range");
        Map<String, Object> quest = Map.of("name", "Epic Quest", "objective", "Save world", "rewardGold", 50_000_000);
        List<RuleViolation> violations = rule.validate(quest);
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).field()).isEqualTo("rewardGold");
    }

    @Test
    void growthCurvePointValueRange_validValue_passes() {
        BusinessRule rule = findRule("growth-curve-point-value-range");
        Map<String, Object> point = Map.of("level", 10, "expRequired", 5000.0);
        List<RuleViolation> violations = rule.validate(point);
        assertThat(violations).isEmpty();
    }

    @Test
    void growthCurvePointValueRange_exceedsMax_violation() {
        BusinessRule rule = findRule("growth-curve-point-value-range");
        Map<String, Object> point = Map.of("level", 10, "expRequired", 5_000_000.0);
        List<RuleViolation> violations = rule.validate(point);
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).field()).isEqualTo("expRequired");
    }

    @Test
    void noNullOrEmptyName_validName_passes() {
        BusinessRule rule = findRule("no-null-or-empty-name");
        Map<String, Object> monster = Map.of("name", "Goblin", "hp", 500, "attack", 50, "level", 5, "dropRate", 0.5);
        List<RuleViolation> violations = rule.validate(monster);
        assertThat(violations).isEmpty();
    }

    @Test
    void noNullOrEmptyName_emptyName_violation() {
        BusinessRule rule = findRule("no-null-or-empty-name");
        Map<String, Object> monster = Map.of("name", "", "hp", 500, "attack", 50, "level", 5, "dropRate", 0.5);
        List<RuleViolation> violations = rule.validate(monster);
        assertThat(violations).hasSize(1);
        assertThat(violations.get(0).field()).isEqualTo("name");
    }

    @Test
    void nonApplicableConfigType_noViolation() {
        BusinessRule rule = findRule("monster-hp-range");
        Map<String, Object> skill = Map.of("name", "Fireball", "damage", 500, "cooldown", 10);
        List<RuleViolation> violations = rule.validate(skill);
        assertThat(violations).isEmpty();
    }
}
