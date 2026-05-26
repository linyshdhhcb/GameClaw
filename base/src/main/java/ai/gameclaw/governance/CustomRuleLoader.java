package ai.gameclaw.governance;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Component
public class CustomRuleLoader {

    private static final Logger log = LoggerFactory.getLogger(CustomRuleLoader.class);

    private final ObjectMapper objectMapper;
    private final Path rulesDir;
    private final List<BusinessRule> customRules = new ArrayList<>();

    public CustomRuleLoader(
            ObjectMapper objectMapper,
            @Value("${gameclaw.workspace:file:./workspace/}") String workspaceStr) {
        this.objectMapper = objectMapper;
        String wsPath = workspaceStr.replace("file:", "").replace("file:///", "/");
        this.rulesDir = Path.of(wsPath).resolve("game-skills").resolve("validation-rules");
    }

    @PostConstruct
    public void loadRules() {
        if (!Files.isDirectory(rulesDir)) {
            log.info("[CustomRuleLoader] Rules directory not found: {}, skipping custom rules", rulesDir);
            return;
        }

        try (Stream<Path> files = Files.list(rulesDir)) {
            files.filter(p -> p.toString().endsWith(".yaml") || p.toString().endsWith(".yml"))
                 .forEach(this::loadRuleFile);
        } catch (IOException e) {
            log.warn("[CustomRuleLoader] Failed to list rules directory: {}", e.getMessage());
        }

        log.info("[CustomRuleLoader] Loaded {} custom rule(s) from {}", customRules.size(), rulesDir);
    }

    @SuppressWarnings("unchecked")
    private void loadRuleFile(Path file) {
        try {
            org.yaml.snakeyaml.Yaml yaml = new org.yaml.snakeyaml.Yaml();
            List<Object> docs;
            try (var is = Files.newInputStream(file)) {
                docs = new ArrayList<>();
                for (Object doc : yaml.loadAll(is)) {
                    docs.add(doc);
                }
            }

            for (Object doc : docs) {
                if (doc instanceof Map<?, ?>) {
                    Map<String, Object> ruleDef = (Map<String, Object>) doc;
                    if (ruleDef.containsKey("rules") && ruleDef.get("rules") instanceof List<?>) {
                        for (Object ruleObj : (List<?>) ruleDef.get("rules")) {
                            if (ruleObj instanceof Map<?, ?>) {
                                loadSingleRule((Map<String, Object>) ruleObj, file);
                            }
                        }
                    } else {
                        loadSingleRule(ruleDef, file);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[CustomRuleLoader] Failed to load rule file {}: {}", file, e.getMessage());
        }
    }

    private void loadSingleRule(Map<String, Object> ruleDef, Path file) {
        String name = (String) ruleDef.getOrDefault("name", file.getFileName().toString());
        String field = (String) ruleDef.get("field");
        String appliesTo = (String) ruleDef.getOrDefault("appliesTo", "Unknown");
        Number min = toNumber(ruleDef.get("min"));
        Number max = toNumber(ruleDef.get("max"));

        if (field == null) {
            log.warn("[CustomRuleLoader] Rule '{}' missing 'field', skipping", name);
            return;
        }

        BusinessRule rule = createRangeRule(name, appliesTo, field, min, max);
        customRules.add(rule);
        log.debug("[CustomRuleLoader] Loaded custom rule: {} (field={}, appliesTo={})", name, field, appliesTo);
    }

    private Number toNumber(Object value) {
        if (value instanceof Number n) {
            return n;
        }
        if (value instanceof String s) {
            return Double.parseDouble(s);
        }
        return null;
    }

    private BusinessRule createRangeRule(String ruleName, String appliesTo, String field,
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
                    if (!(value instanceof Number numValue)) {
                        continue;
                    }
                    if (min != null && numValue.doubleValue() < min.doubleValue()) {
                        violations.add(new RuleViolation(ruleName, field,
                                "Value " + numValue + " is below minimum " + min));
                    }
                    if (max != null && numValue.doubleValue() > max.doubleValue()) {
                        violations.add(new RuleViolation(ruleName, field,
                                "Value " + numValue + " exceeds maximum " + max));
                    }
                }
                return violations;
            }

            @SuppressWarnings("unchecked")
            private Map<String, Object> toMap(Object output) {
                if (output == null) {
                    return Map.of();
                }
                if (output instanceof Map<?, ?> m) {
                    return (Map<String, Object>) m;
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
        };
    }

    public List<BusinessRule> getCustomRules() {
        return Collections.unmodifiableList(customRules);
    }
}
