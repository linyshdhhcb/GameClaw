package ai.gameclaw.governance;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class DefaultBusinessRules {

    private static final Logger log = LoggerFactory.getLogger(DefaultBusinessRules.class);

    private final ObjectMapper objectMapper;

    public DefaultBusinessRules(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    private Map<String, Object> toMap(Object output) {
        if (output == null) {
            return Map.of();
        }
        if (output instanceof Map<?, ?> m) {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) m;
            return result;
        }
        return objectMapper.convertValue(output, LinkedHashMap.class);
    }

    private List<Map<String, Object>> toMapList(Object output) {
        if (output instanceof List<?> list) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : list) {
                result.add(toMap(item));
            }
            return result;
        }
        return Collections.singletonList(toMap(output));
    }

    private String detectConfigType(Map<String, Object> map) {
        Object typeField = map.get("@type");
        if (typeField instanceof String typeName) {
            return typeName;
        }
        if (map.containsKey("dropRate") || map.containsKey("drop_rate")) {
            return "MonsterConfig";
        }
        if (map.containsKey("cooldown") || map.containsKey("damage")) {
            return "SkillConfig";
        }
        if (map.containsKey("rarity") || map.containsKey("price")) {
            return "ItemConfig";
        }
        if (map.containsKey("objective") || map.containsKey("rewardGold") || map.containsKey("reward_gold")) {
            return "QuestConfig";
        }
        if (map.containsKey("expRequired") || map.containsKey("exp_required")) {
            return "GrowthCurvePoint";
        }
        return "Unknown";
    }

    private BusinessRule numericRangeRule(String ruleName, String appliesTo, String field,
                                          Number min, Number max) {
        return new BusinessRule() {
            @Override
            public String name() {
                return ruleName;
            }

            @Override
            public List<RuleViolation> validate(Object output) {
                List<RuleViolation> violations = new ArrayList<>();
                for (Map<String, Object> map : toMapList(output)) {
                    String configType = detectConfigType(map);
                    if (!appliesTo.equals(configType)) {
                        continue;
                    }
                    Object value = map.get(field);
                    if (value == null) {
                        value = map.get(camelToSnake(field));
                    }
                    if (value == null) {
                        continue;
                    }
                    double numValue = ((Number) value).doubleValue();
                    if (numValue < min.doubleValue() || numValue > max.doubleValue()) {
                        violations.add(new RuleViolation(ruleName, field,
                                "Value " + numValue + " is out of range [" + min + ", " + max + "]"));
                    }
                }
                return violations;
            }
        };
    }

    private String camelToSnake(String camel) {
        StringBuilder sb = new StringBuilder();
        for (char c : camel.toCharArray()) {
            if (Character.isUpperCase(c)) {
                sb.append('_').append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    @Bean
    public BusinessRule monsterHpRangeRule() {
        return numericRangeRule("monster-hp-range", "MonsterConfig", "hp", 1, 1_000_000);
    }

    @Bean
    public BusinessRule monsterAttackRangeRule() {
        return numericRangeRule("monster-attack-range", "MonsterConfig", "attack", 1, 100_000);
    }

    @Bean
    public BusinessRule monsterLevelRangeRule() {
        return numericRangeRule("monster-level-range", "MonsterConfig", "level", 1, 100);
    }

    @Bean
    public BusinessRule monsterDropRateRangeRule() {
        return numericRangeRule("monster-drop-rate-range", "MonsterConfig", "dropRate", 0.0, 1.0);
    }

    @Bean
    public BusinessRule skillDamageRangeRule() {
        return numericRangeRule("skill-damage-range", "SkillConfig", "damage", 1, 1_000_000);
    }

    @Bean
    public BusinessRule skillCooldownRangeRule() {
        return numericRangeRule("skill-cooldown-range", "SkillConfig", "cooldown", 0, 3600);
    }

    @Bean
    public BusinessRule itemPriceRangeRule() {
        return numericRangeRule("item-price-range", "ItemConfig", "price", 0, 10_000_000);
    }

    @Bean
    public BusinessRule questRewardGoldRangeRule() {
        return numericRangeRule("quest-reward-gold-range", "QuestConfig", "rewardGold", 0, 10_000_000);
    }

    @Bean
    public BusinessRule growthCurvePointValueRangeRule() {
        return numericRangeRule("growth-curve-point-value-range", "GrowthCurvePoint", "expRequired", 0.01, 1_000_000);
    }

    @Bean
    public BusinessRule noNullOrEmptyNameRule() {
        return new BusinessRule() {
            @Override
            public String name() {
                return "no-null-or-empty-name";
            }

            @Override
            public List<RuleViolation> validate(Object output) {
                List<RuleViolation> violations = new ArrayList<>();
                for (Map<String, Object> map : toMapList(output)) {
                    Object nameValue = map.get("name");
                    if (nameValue == null) {
                        nameValue = map.get("name");
                    }
                    if (nameValue == null || (nameValue instanceof String s && s.isBlank())) {
                        violations.add(new RuleViolation("no-null-or-empty-name", "name",
                                "Name field must not be null or empty"));
                    }
                }
                return violations;
            }
        };
    }
}
