package ai.gameclaw.skills;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

@Component
public class GameClawSkillsLoader implements SkillRegistry {

    private static final Logger log = LoggerFactory.getLogger(GameClawSkillsLoader.class);

    private final GameClawSkillParser parser;
    private final Path workspace;
    private final Cache<String, GameClawSkill> cache;

    private volatile Map<String, GameClawSkill> loadedSkills = Collections.emptyMap();

    public GameClawSkillsLoader(
            GameClawSkillParser parser,
            @Value("${gameclaw.workspace:file:./workspace/}") String workspaceStr) {
        this.parser = parser;
        String wsPath = workspaceStr.replace("file:", "").replace("file:///", "/");
        this.workspace = Path.of(wsPath).resolve("skills");
        this.cache = Caffeine.newBuilder()
                .maximumSize(200)
                .expireAfterAccess(Duration.ofMinutes(30))
                .build();
    }

    public Map<String, GameClawSkill> loadAll() {
        Map<String, GameClawSkill> skills = new LinkedHashMap<>();

        loadFromClasspath(skills);
        loadFromDirectory(Path.of(System.getProperty("user.home"), ".openclaw", "skills"), skills);
        loadFromDirectory(Path.of(System.getProperty("user.home"), ".gameclaw", "skills"), skills);
        loadFromDirectory(workspace, skills);

        loadedSkills = Collections.unmodifiableMap(skills);
        skills.forEach((name, skill) -> cache.put(name, skill));
        log.info("[Skills] Loaded {} skills", skills.size());
        return loadedSkills;
    }

    public GameClawSkill getLoadedSkill(String name) {
        GameClawSkill cached = cache.getIfPresent(name);
        if (cached != null) return cached;
        return loadedSkills.get(name);
    }

    public void reload() {
        cache.invalidateAll();
        loadAll();
    }

    @Override
    public void registerSkill(SkillDefinition skill) {
        throw new UnsupportedOperationException("Use GameClawSkillParser + loadAll() for skill registration");
    }

    @Override
    public SkillDefinition getSkill(String name) {
        GameClawSkill skill = getLoadedSkill(name);
        return skill != null ? new SkillDefinition(skill.name(), skill.description(), skill.instructions()) : null;
    }

    @Override
    public List<SkillDefinition> getAllSkills() {
        return loadedSkills.values().stream()
                .map(s -> new SkillDefinition(s.name(), s.description(), s.instructions()))
                .toList();
    }

    private void loadFromClasspath(Map<String, GameClawSkill> sink) {
        try {
            var resource = getClass().getClassLoader().getResource("skills");
            if (resource == null) return;
            // Classpath skills are bundled, skip for now
            log.debug("[Skills] Classpath skills loading not yet implemented");
        } catch (Exception e) {
            log.debug("[Skills] No classpath skills found");
        }
    }

    private void loadFromDirectory(Path dir, Map<String, GameClawSkill> sink) {
        if (!Files.isDirectory(dir)) return;
        log.debug("[Skills] Scanning directory: {}", dir);
        try (Stream<Path> walk = Files.walk(dir, 3)) {
            walk.filter(p -> p.getFileName().toString().equals("SKILL.md"))
                    .map(parser::parse)
                    .filter(Objects::nonNull)
                    .forEach(s -> sink.put(s.name(), s));
        } catch (IOException e) {
            log.warn("[Skills] Failed to scan {}: {}", dir, e.getMessage());
        }
    }
}
