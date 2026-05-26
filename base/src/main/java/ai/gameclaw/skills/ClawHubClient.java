package ai.gameclaw.skills;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;

@Component
@ConditionalOnProperty(name = "gameclaw.clawhub.enabled", havingValue = "true", matchIfMissing = false)
@EnableConfigurationProperties(ClawHubProperties.class)
public class ClawHubClient {

    private static final Logger log = LoggerFactory.getLogger(ClawHubClient.class);

    private final ClawHubProperties properties;
    private final GameClawSkillsLoader skillsLoader;
    private final ApplicationEventPublisher eventPublisher;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public ClawHubClient(
            ClawHubProperties properties,
            GameClawSkillsLoader skillsLoader,
            ApplicationEventPublisher eventPublisher) {
        this.properties = properties;
        this.skillsLoader = skillsLoader;
        this.eventPublisher = eventPublisher;
        this.restClient = RestClient.builder()
                .baseUrl(properties.registryUrl())
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public Path install(String skillName, String version) {
        try {
            log.info("[ClawHub] Installing skill {}@{}", skillName, version);
            byte[] zipBytes = restClient.get()
                    .uri("/api/v1/skills/{name}/{version}/download", skillName, version)
                    .retrieve()
                    .body(byte[].class);

            if (zipBytes == null || zipBytes.length == 0) {
                throw new IllegalStateException("Empty response received for skill " + skillName + "@" + version);
            }

            Path installDir = Path.of(properties.installDir()).resolve(skillName);
            Files.createDirectories(installDir);
            extractZip(zipBytes, installDir);

            skillsLoader.reload();
            eventPublisher.publishEvent(new SkillInstalledEvent(this, skillName, version, installDir));

            log.info("[ClawHub] Skill {}@{} installed to {}", skillName, version, installDir);
            return installDir;
        } catch (Exception e) {
            String msg = "Failed to install skill " + skillName + "@" + version + ": " + e.getMessage();
            log.error("[ClawHub] {}", msg);
            throw new ClawHubException(msg, e);
        }
    }

    public List<ClawHubSearchResult> search(String query) {
        try {
            log.info("[ClawHub] Searching for skills: {}", query);
            String json = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/skills/search")
                            .queryParam("q", query)
                            .build())
                    .retrieve()
                    .body(String.class);

            if (json == null || json.isBlank()) {
                return Collections.emptyList();
            }
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            String msg = "Failed to search skills for query '" + query + "': " + e.getMessage();
            log.error("[ClawHub] {}", msg);
            throw new ClawHubException(msg, e);
        }
    }

    public String update(String skillName) {
        try {
            log.info("[ClawHub] Checking for updates: {}", skillName);
            Map<String, Object> latest = restClient.get()
                    .uri("/api/v1/skills/{name}/latest", skillName)
                    .retrieve()
                    .body(Map.class);

            if (latest == null) {
                throw new ClawHubException("No version info found for skill: " + skillName);
            }

            String latestVersion = String.valueOf(latest.get("version"));
            install(skillName, latestVersion);
            log.info("[ClawHub] Skill {} updated to version {}", skillName, latestVersion);
            return latestVersion;
        } catch (ClawHubException e) {
            throw e;
        } catch (Exception e) {
            String msg = "Failed to update skill " + skillName + ": " + e.getMessage();
            log.error("[ClawHub] {}", msg);
            throw new ClawHubException(msg, e);
        }
    }

    public void updateAll() {
        Map<String, GameClawSkill> skills = skillsLoader.loadAll();
        for (String skillName : skills.keySet()) {
            try {
                update(skillName);
            } catch (ClawHubException e) {
                log.warn("[ClawHub] Skipping update for {}: {}", skillName, e.getMessage());
            }
        }
    }

    private void extractZip(byte[] zipBytes, Path targetDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            java.util.zip.ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path entryPath = targetDir.resolve(entry.getName()).normalize();
                if (!entryPath.startsWith(targetDir.normalize())) {
                    throw new IOException("Zip entry escapes target directory: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Files.createDirectories(entryPath.getParent());
                    Files.copy(zis, entryPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
        }
    }

    public static class ClawHubException extends RuntimeException {
        public ClawHubException(String message, Throwable cause) {
            super(message, cause);
        }

        public ClawHubException(String message) {
            super(message);
        }
    }
}
