package ai.gameclaw.compat.openclaw;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PluginClassLoaderTest {

    @Test
    void blocksInternalGameClawClasses() {
        PluginClassLoader loader = new PluginClassLoader(
                new java.net.URL[0], getClass().getClassLoader(), "test", List.of()
        );

        assertThatThrownBy(() -> loader.loadClass("ai.gameclaw.agent.llm.LlmClient"))
                .isInstanceOf(ClassNotFoundException.class)
                .hasMessageContaining("internal class");

        assertThatThrownBy(() -> loader.loadClass("ai.gameclaw.security.TenantContextHolder"))
                .isInstanceOf(ClassNotFoundException.class)
                .hasMessageContaining("internal class");
    }

    @Test
    void allowsCompatOpenclawPackage() throws ClassNotFoundException {
        PluginClassLoader loader = new PluginClassLoader(
                new java.net.URL[0], getClass().getClassLoader(), "test", List.of()
        );

        Class<?> cls = loader.loadClass("ai.gameclaw.compat.openclaw.OpenClawTool");
        assertThat(cls).isNotNull();
    }

    @Test
    void allowsStandardLibraryClasses() throws ClassNotFoundException {
        PluginClassLoader loader = new PluginClassLoader(
                new java.net.URL[0], getClass().getClassLoader(), "test", List.of()
        );

        Class<?> stringClass = loader.loadClass("java.lang.String");
        assertThat(stringClass).isNotNull();
    }

    @Test
    void hasPermission_wildcard() {
        PluginClassLoader loader = new PluginClassLoader(
                new java.net.URL[0], getClass().getClassLoader(), "test", List.of("*")
        );

        assertThat(loader.hasPermission("fs:read:workspace/")).isTrue();
        assertThat(loader.hasPermission("net:host:api.example.com")).isTrue();
    }

    @Test
    void hasPermission_specificPrefix() {
        PluginClassLoader loader = new PluginClassLoader(
                new java.net.URL[0], getClass().getClassLoader(), "test",
                List.of("fs:read:workspace/", "net:host:api.feishu.cn")
        );

        assertThat(loader.hasPermission("fs:read:workspace/data/")).isTrue();
        assertThat(loader.hasPermission("net:host:api.feishu.cn")).isTrue();
        assertThat(loader.hasPermission("fs:write:workspace/")).isFalse();
    }

    @Test
    void hasPermission_emptyPermissions() {
        PluginClassLoader loader = new PluginClassLoader(
                new java.net.URL[0], getClass().getClassLoader(), "test", List.of()
        );

        assertThat(loader.hasPermission("fs:read:workspace/")).isFalse();
    }
}
