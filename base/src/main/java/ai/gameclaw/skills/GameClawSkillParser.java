package ai.gameclaw.skills;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class GameClawSkillParser {

    private static final Logger log = LoggerFactory.getLogger(GameClawSkillParser.class);

    public GameClawSkill parse(Path skillMdPath) {
        String content;
        try {
            content = Files.readString(skillMdPath);
        } catch (IOException e) {
            log.warn("[Skills] Failed to read {}: {}", skillMdPath, e.getMessage());
            return null;
        }

        Path baseDir = skillMdPath.getParent();
        Map<String, Object> frontmatter = parseFrontmatter(content);
        String body = stripFrontmatter(content);

        if (frontmatter == null) {
            frontmatter = Collections.emptyMap();
        }

        String name = getString(frontmatter, "name");
        if (name == null) {
            log.warn("[Skills] Missing 'name' in {} — skipped", skillMdPath);
            return null;
        }

        String description = getString(frontmatter, "description");

        Map<String, Object> metadata = getMap(frontmatter, "metadata");
        boolean userInvocable = getBoolean(frontmatter, "user-invocable", false);
        boolean disableModelInvocation = getBoolean(frontmatter, "disable-model-invocation", false);
        String commandDispatch = getString(frontmatter, "command-dispatch");
        String commandTool = getString(frontmatter, "command-tool");
        String commandArgMode = getString(frontmatter, "command-arg-mode");

        if (metadata != null && metadata.containsKey("gate")) {
            log.info("[Skills] metadata.gate not supported in Phase 0 (parsed and stored): {}", name);
        }
        if (userInvocable) {
            log.info("[Skills] user-invocable parsed; will be enabled in Phase 2: {}", name);
        }
        if (commandDispatch != null) {
            log.info("[Skills] command-dispatch parsed; tool dispatch mode: {}: {}", commandDispatch, name);
        }

        Map<String, Path> resources = indexResources(baseDir);

        return new GameClawSkill(
                name, description, body, metadata,
                userInvocable, disableModelInvocation,
                commandDispatch, commandTool, commandArgMode,
                baseDir, resources
        );
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseFrontmatter(String content) {
        if (!content.startsWith("---")) {
            return null;
        }
        int closingIdx = findClosingDelimiter(content, 3);
        if (closingIdx < 0) {
            return null;
        }
        String yamlBlock = content.substring(3, closingIdx);
        try {
            Yaml yaml = new Yaml();
            return yaml.load(yamlBlock);
        } catch (Exception e) {
            log.warn("[Skills] Failed to parse YAML frontmatter: {}", e.getMessage());
            return null;
        }
    }

    private String stripFrontmatter(String content) {
        if (!content.startsWith("---")) {
            return content;
        }
        int closingIdx = findClosingDelimiter(content, 3);
        if (closingIdx < 0) {
            return "";
        }
        int bodyStart = closingIdx + 3;
        if (bodyStart < content.length() && content.charAt(bodyStart) == '\n') {
            bodyStart++;
        }
        return bodyStart < content.length() ? content.substring(bodyStart) : "";
    }

    private int findClosingDelimiter(String content, int fromIdx) {
        int idx = fromIdx;
        while (idx < content.length()) {
            if (content.startsWith("---", idx)) {
                boolean atLineStart = (idx == 0 || content.charAt(idx - 1) == '\n');
                boolean atLineEnd = (idx + 3 >= content.length() || content.charAt(idx + 3) == '\n' || content.charAt(idx + 3) == '\r');
                if (atLineStart && atLineEnd) {
                    return idx;
                }
            }
            idx++;
        }
        return -1;
    }

    private String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }

    private boolean getBoolean(Map<String, Object> map, String key, boolean defaultVal) {
        Object val = map.get(key);
        return val != null ? Boolean.parseBoolean(val.toString()) : defaultVal;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getMap(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof Map) {
            return new LinkedHashMap<>((Map<String, Object>) val);
        }
        return null;
    }

    private Map<String, Path> indexResources(Path baseDir) {
        Map<String, Path> resources = new LinkedHashMap<>();
        if (baseDir == null || !Files.isDirectory(baseDir)) {
            return resources;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(baseDir)) {
            for (Path entry : stream) {
                if (!Files.isRegularFile(entry)) continue;
                String fileName = entry.getFileName().toString();
                if (fileName.equals("SKILL.md")) continue;
                Path absEntry = entry.toAbsolutePath();
                Path absBase = baseDir.toAbsolutePath();
                if (absEntry.startsWith(absBase)) {
                    resources.put(fileName, entry);
                }
            }
        } catch (IOException e) {
            log.warn("[Skills] Failed to index resources in {}: {}", baseDir, e.getMessage());
        }
        return resources;
    }
}
