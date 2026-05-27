package ai.gameclaw.compat.openclaw;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PluginManifestReaderTest {

    private PluginManifestReader reader;
    private ObjectMapper objectMapper;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        reader = new PluginManifestReader(objectMapper);
    }

    @Test
    void readFromDirectory_validManifest() throws IOException {
        String json = """
                {
                  "name": "test-plugin",
                  "version": "1.0.0",
                  "main": "com.example.TestPlugin",
                  "permissions": ["fs:read:workspace/", "net:host:api.example.com"],
                  "tools": ["search_doc"],
                  "minOpenClawVersion": "0.9.0"
                }
                """;
        Path pluginDir = tempDir.resolve("test-plugin");
        Files.createDirectories(pluginDir);
        Files.writeString(pluginDir.resolve("plugin.json"), json);

        PluginManifest manifest = reader.readFromDirectory(pluginDir);

        assertThat(manifest.name()).isEqualTo("test-plugin");
        assertThat(manifest.version()).isEqualTo("1.0.0");
        assertThat(manifest.main()).isEqualTo("com.example.TestPlugin");
        assertThat(manifest.permissions()).containsExactly("fs:read:workspace/", "net:host:api.example.com");
        assertThat(manifest.declaredTools()).containsExactly("search_doc");
        assertThat(manifest.minOpenClawVersion()).isEqualTo("0.9.0");
    }

    @Test
    void readFromDirectory_missingName_throws() throws IOException {
        String json = """
                {
                  "version": "1.0.0",
                  "main": "com.example.TestPlugin"
                }
                """;
        Path pluginDir = tempDir.resolve("bad-plugin");
        Files.createDirectories(pluginDir);
        Files.writeString(pluginDir.resolve("plugin.json"), json);

        assertThatThrownBy(() -> reader.readFromDirectory(pluginDir))
                .hasRootCauseInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void readFromDirectory_missingManifest_throws() {
        Path emptyDir = tempDir.resolve("empty-plugin");
        assertThatThrownBy(() -> reader.readFromDirectory(emptyDir))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("plugin.json not found");
    }

    @Test
    void readFromDirectory_defaultsForOptionalFields() throws IOException {
        String json = """
                {
                  "name": "minimal-plugin",
                  "version": "0.1.0",
                  "main": "com.example.MinimalPlugin"
                }
                """;
        Path pluginDir = tempDir.resolve("minimal-plugin");
        Files.createDirectories(pluginDir);
        Files.writeString(pluginDir.resolve("plugin.json"), json);

        PluginManifest manifest = reader.readFromDirectory(pluginDir);

        assertThat(manifest.permissions()).isEmpty();
        assertThat(manifest.declaredTools()).isEmpty();
        assertThat(manifest.minOpenClawVersion()).isNull();
    }
}
