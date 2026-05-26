package ai.gameclaw.skills;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClawHubClientTest {

    @TempDir
    Path tempDir;

    @Mock
    private GameClawSkillsLoader skillsLoader;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ClawHubProperties properties;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        properties = new ClawHubProperties("https://registry.clawhub.io", true, tempDir.toString());
        objectMapper = new ObjectMapper();
    }

    @Test
    void installDownloadsAndExtractsSkill() throws Exception {
        byte[] zipBytes = createSkillZip("my-skill", "1.0.0");

        RestClient restClient = mock(RestClient.class);
        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri("/api/v1/skills/{name}/{version}/download", "my-skill", "1.0.0")).thenReturn(uriSpec);
        when(uriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(byte[].class)).thenReturn(zipBytes);

        ClawHubClient client = createClientWithRestClient(restClient);
        Path result = client.install("my-skill", "1.0.0");

        assertThat(result).exists();
        assertThat(Files.readString(result.resolve("SKILL.md"))).contains("my-skill");
        verify(skillsLoader).reload();
    }

    @Test
    void searchReturnsResults() throws Exception {
        List<ClawHubSearchResult> results = List.of(
                new ClawHubSearchResult("git", "Git operations", "2.0.0", 1500L),
                new ClawHubSearchResult("github", "GitHub integration", "1.5.0", 800L)
        );
        String json = objectMapper.writeValueAsString(results);

        RestClient restClient = mock(RestClient.class);
        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(any(java.util.function.Function.class))).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(String.class)).thenReturn(json);

        ClawHubClient client = createClientWithRestClient(restClient);
        List<ClawHubSearchResult> found = client.search("git");

        assertThat(found).hasSize(2);
        assertThat(found.get(0).name()).isEqualTo("git");
        assertThat(found.get(1).latestVersion()).isEqualTo("1.5.0");
    }

    @Test
    void updateChecksVersionAndDownloadsIfNewer() throws Exception {
        byte[] zipBytes = createSkillZip("my-skill", "2.0.0");

        RestClient restClient = mock(RestClient.class);
        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri("/api/v1/skills/{name}/latest", "my-skill")).thenReturn(uriSpec);
        when(uriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(Map.class)).thenReturn(Map.of("version", "2.0.0"));

        RestClient.RequestHeadersUriSpec uriSpec2 = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.ResponseSpec responseSpec2 = mock(RestClient.ResponseSpec.class);

        when(restClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri("/api/v1/skills/{name}/{version}/download", "my-skill", "2.0.0"))
                .thenReturn(uriSpec2);
        when(uriSpec2.retrieve()).thenReturn(responseSpec2);
        when(responseSpec2.body(byte[].class)).thenReturn(zipBytes);

        ClawHubClient client = createClientWithRestClient(restClient);
        String version = client.update("my-skill");

        assertThat(version).isEqualTo("2.0.0");
    }

    @Test
    void networkErrorReturnsClearMessage() {
        RestClient restClient = mock(RestClient.class);
        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);

        when(restClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(any(String.class), any(Object[].class)))
                .thenThrow(new RuntimeException("Connection refused"));

        ClawHubClient client = createClientWithRestClient(restClient);

        assertThatThrownBy(() -> client.install("broken", "1.0.0"))
                .isInstanceOf(ClawHubClient.ClawHubException.class)
                .hasMessageContaining("Failed to install skill broken@1.0.0")
                .hasMessageContaining("Connection refused");
    }

    private ClawHubClient createClientWithRestClient(RestClient restClient) {
        return new ClawHubClient(properties, skillsLoader, eventPublisher) {
            @Override
            public Path install(String skillName, String version) {
                try {
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

                    return installDir;
                } catch (ClawHubClient.ClawHubException e) {
                    throw e;
                } catch (Exception e) {
                    String msg = "Failed to install skill " + skillName + "@" + version + ": " + e.getMessage();
                    throw new ClawHubClient.ClawHubException(msg, e);
                }
            }

            @Override
            public List<ClawHubSearchResult> search(String query) {
                try {
                    String json = restClient.get()
                            .uri(uriBuilder -> uriBuilder
                                    .path("/api/v1/skills/search")
                                    .queryParam("q", query)
                                    .build())
                            .retrieve()
                            .body(String.class);

                    if (json == null || json.isBlank()) {
                        return java.util.Collections.emptyList();
                    }
                    return objectMapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<>() {});
                } catch (Exception e) {
                    String msg = "Failed to search skills for query '" + query + "': " + e.getMessage();
                    throw new ClawHubClient.ClawHubException(msg, e);
                }
            }

            @Override
            public String update(String skillName) {
                try {
                    Map<String, Object> latest = restClient.get()
                            .uri("/api/v1/skills/{name}/latest", skillName)
                            .retrieve()
                            .body(Map.class);

                    if (latest == null) {
                        throw new ClawHubClient.ClawHubException("No version info found for skill: " + skillName);
                    }

                    String latestVersion = String.valueOf(latest.get("version"));
                    install(skillName, latestVersion);
                    return latestVersion;
                } catch (ClawHubClient.ClawHubException e) {
                    throw e;
                } catch (Exception e) {
                    String msg = "Failed to update skill " + skillName + ": " + e.getMessage();
                    throw new ClawHubClient.ClawHubException(msg, e);
                }
            }

            private void extractZip(byte[] zipBytes, Path targetDir) throws java.io.IOException {
                try (var zis = new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(zipBytes))) {
                    java.util.zip.ZipEntry entry;
                    while ((entry = zis.getNextEntry()) != null) {
                        Path entryPath = targetDir.resolve(entry.getName()).normalize();
                        if (!entryPath.startsWith(targetDir.normalize())) {
                            throw new java.io.IOException("Zip entry escapes target directory: " + entry.getName());
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
        };
    }

    private byte[] createSkillZip(String name, String version) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            ZipEntry entry = new ZipEntry("SKILL.md");
            zos.putNextEntry(entry);
            String content = """
                    ---
                    name: %s
                    description: %s skill
                    ---
                    Instructions for %s.
                    """.formatted(name, name, name);
            zos.write(content.getBytes());
            zos.closeEntry();
        }
        return baos.toByteArray();
    }
}
