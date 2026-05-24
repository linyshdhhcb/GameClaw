package ai.gameclaw.configuration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class DualFormatConfigurationManager extends ConfigurationManager {

    private static final Logger logger = LoggerFactory.getLogger(DualFormatConfigurationManager.class);

    private final ObjectMapper objectMapper;

    public DualFormatConfigurationManager(Environment environment,
                                          ApplicationEventPublisher eventPublisher) {
        super(environment, eventPublisher);
        this.objectMapper = new ObjectMapper();
    }

    public Map<String, Object> readOpenClawJson() throws IOException {
        Path openClawJsonPath = resolveOpenClawJsonPath();
        if (!Files.exists(openClawJsonPath)) {
            return Map.of();
        }
        JsonNode root = objectMapper.readTree(openClawJsonPath.toFile());
        return objectMapper.convertValue(root, LinkedHashMap.class);
    }

    public void writeOpenClawJson(Map<String, Object> config) throws IOException {
        Path openClawJsonPath = resolveOpenClawJsonPath();
        Files.createDirectories(openClawJsonPath.getParent());
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(openClawJsonPath.toFile(), config);
        logger.info("Written openclaw.json to {}", openClawJsonPath);
    }

    public Map<String, Object> readMergedConfiguration() throws IOException {
        Map<String, Object> yamlConfig = readApplicationYaml();
        Map<String, Object> jsonConfig = readOpenClawJson();
        Map<String, Object> merged = new LinkedHashMap<>(yamlConfig);
        deepMerge(merged, jsonConfig);
        return merged;
    }

    @Override
    public void updateProperty(String key, Object value) throws IOException {
        super.updateProperty(key, value);
        syncOpenClawJsonFromYaml();
    }

    @Override
    public void updateProperties(Map<String, Object> keyValues) throws IOException {
        super.updateProperties(keyValues);
        syncOpenClawJsonFromYaml();
    }

    private void syncOpenClawJsonFromYaml() throws IOException {
        Map<String, Object> yamlConfig = readApplicationYaml();
        Map<String, Object> jsonConfig = new LinkedHashMap<>();
        extractOpenClawCompatibleEntries(yamlConfig, jsonConfig);
        if (!jsonConfig.isEmpty()) {
            writeOpenClawJson(jsonConfig);
        }
    }

    @SuppressWarnings("unchecked")
    private void extractOpenClawCompatibleEntries(Map<String, Object> source, Map<String, Object> target) {
        if (source.containsKey("spring") && source.get("spring") instanceof Map spring) {
            if (spring.containsKey("ai") && spring.get("ai") instanceof Map ai) {
                target.put("ai", ai);
            }
        }
        if (source.containsKey("agent") && source.get("agent") instanceof Map agent) {
            if (agent.containsKey("channels") && agent.get("channels") instanceof Map channels) {
                target.put("channels", channels);
            }
            if (agent.containsKey("skills") && agent.get("skills") instanceof Map skills) {
                target.put("skills", skills);
            }
        }
    }

    private Path resolveOpenClawJsonPath() {
        return Path.of("workspace", "openclaw.json");
    }

    @SuppressWarnings("unchecked")
    private static void deepMerge(Map<String, Object> base, Map<String, Object> override) {
        override.forEach((key, value) -> {
            if (value instanceof Map && base.get(key) instanceof Map) {
                deepMerge((Map<String, Object>) base.get(key), (Map<String, Object>) value);
            } else {
                base.put(key, value);
            }
        });
    }
}
