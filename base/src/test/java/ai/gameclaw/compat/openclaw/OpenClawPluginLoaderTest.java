package ai.gameclaw.compat.openclaw;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.ApplicationEventPublisher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class OpenClawPluginLoaderTest {

    @TempDir
    Path tempDir;

    private OpenClawPluginLoader loader;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        loader = new OpenClawPluginLoader(objectMapper, publisher, tempDir.toString());
    }

    @Test
    void loadFromDirectory_validPlugin() throws IOException, PluginLoadException {
        Path pluginDir = tempDir.resolve("plugins").resolve("sample-plugin");
        Files.createDirectories(pluginDir);

        String manifest = """
                {
                  "name": "sample-plugin",
                  "version": "1.0.0",
                  "main": "ai.gameclaw.compat.openclaw.SampleTestPlugin",
                  "permissions": ["fs:read:workspace/"],
                  "tools": []
                }
                """;
        Files.writeString(pluginDir.resolve("plugin.json"), manifest);

        PluginInstance instance = loader.loadFromDirectory(pluginDir);
        assertThat(instance.name()).isEqualTo("sample-plugin");
        assertThat(instance.version()).isEqualTo("1.0.0");
    }

    @Test
    void loadFromDirectory_duplicatePlugin_throws() throws IOException, PluginLoadException {
        Path pluginDir = tempDir.resolve("plugins").resolve("dup-plugin");
        Files.createDirectories(pluginDir);

        String manifest = """
                {
                  "name": "dup-plugin",
                  "version": "1.0.0",
                  "main": "ai.gameclaw.compat.openclaw.SampleTestPlugin"
                }
                """;
        Files.writeString(pluginDir.resolve("plugin.json"), manifest);

        loader.loadFromDirectory(pluginDir);

        assertThatThrownBy(() -> loader.loadFromDirectory(pluginDir))
                .isInstanceOf(PluginLoadException.class)
                .hasMessageContaining("already loaded");
    }

    @Test
    void loadFromDirectory_missingMainClass_throws() throws IOException {
        Path pluginDir = tempDir.resolve("plugins").resolve("missing-main");
        Files.createDirectories(pluginDir);

        String manifest = """
                {
                  "name": "missing-main",
                  "version": "1.0.0",
                  "main": "com.nonexistent.Plugin"
                }
                """;
        Files.writeString(pluginDir.resolve("plugin.json"), manifest);

        assertThatThrownBy(() -> loader.loadFromDirectory(pluginDir))
                .isInstanceOf(PluginLoadException.class)
                .hasMessageContaining("main class not found");
    }

    @Test
    void unload_removesPlugin() throws IOException, PluginLoadException {
        Path pluginDir = tempDir.resolve("plugins").resolve("unload-plugin");
        Files.createDirectories(pluginDir);

        String manifest = """
                {
                  "name": "unload-plugin",
                  "version": "1.0.0",
                  "main": "ai.gameclaw.compat.openclaw.SampleTestPlugin"
                }
                """;
        Files.writeString(pluginDir.resolve("plugin.json"), manifest);

        loader.loadFromDirectory(pluginDir);
        assertThat(loader.getPlugin("unload-plugin")).isNotNull();

        loader.unload("unload-plugin");
        assertThat(loader.getPlugin("unload-plugin")).isNull();
    }

    @Test
    void getAllPlugins_returnsUnmodifiableMap() throws IOException, PluginLoadException {
        Path pluginDir = tempDir.resolve("plugins").resolve("map-plugin");
        Files.createDirectories(pluginDir);

        String manifest = """
                {
                  "name": "map-plugin",
                  "version": "1.0.0",
                  "main": "ai.gameclaw.compat.openclaw.SampleTestPlugin"
                }
                """;
        Files.writeString(pluginDir.resolve("plugin.json"), manifest);

        loader.loadFromDirectory(pluginDir);

        assertThatThrownBy(() -> loader.getAllPlugins().put("hack", null))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
