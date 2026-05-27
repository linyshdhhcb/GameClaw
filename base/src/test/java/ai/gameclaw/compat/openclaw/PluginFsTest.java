package ai.gameclaw.compat.openclaw;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PluginFsTest {

    @TempDir
    Path tempDir;

    @Test
    void resolveSafe_withinPluginRoot() throws Exception {
        Path pluginRoot = PluginFs.pluginRoot(tempDir, "my-plugin");
        Files.createDirectories(pluginRoot.resolve("data"));

        Path resolved = PluginFs.resolveSafe(tempDir, "my-plugin", "data/config.json");
        assertThat(resolved.toString()).contains("data");
        assertThat(PluginFs.isWithinPlugin(tempDir, "my-plugin", resolved)).isTrue();
    }

    @Test
    void resolveSafe_pathEscape_throws() {
        assertThatThrownBy(() -> PluginFs.resolveSafe(tempDir, "my-plugin", "../../etc/passwd"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("path escape");
    }

    @Test
    void resolveSafe_absolutePathEscape_throws() {
        assertThatThrownBy(() -> PluginFs.resolveSafe(tempDir, "my-plugin", "/etc/passwd"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("path escape");
    }

    @Test
    void resolveSafe_dotDotEscape_throws() {
        assertThatThrownBy(() -> PluginFs.resolveSafe(tempDir, "my-plugin", "data/../../../etc/shadow"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("path escape");
    }

    @Test
    void isWithinPlugin_withinBounds() {
        Path root = PluginFs.pluginRoot(tempDir, "my-plugin");
        Path file = root.resolve("data/test.json");
        assertThat(PluginFs.isWithinPlugin(tempDir, "my-plugin", file)).isTrue();
    }

    @Test
    void isWithinPlugin_outOfBounds() {
        Path outside = tempDir.resolve("other-plugin").resolve("data.json");
        assertThat(PluginFs.isWithinPlugin(tempDir, "my-plugin", outside)).isFalse();
    }
}
