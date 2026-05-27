package ai.gameclaw.compat.openclaw;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PluginAwareHttpClientTest {

    @Test
    void noPermissions_blocksNetwork() {
        PluginAwareHttpClient client = new PluginAwareHttpClient("test-plugin", List.of());

        assertThatThrownBy(() -> client.get("https://api.example.com/data"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("no network permissions");
    }

    @Test
    void wildcardPermission_allowsAnyHost() {
        PluginAwareHttpClient client = new PluginAwareHttpClient("test-plugin", List.of("*"));

        assertThatCodeDoesNotThrowSecurity(() -> client.assertNetworkAllowed("https://api.example.com/data"));
    }

    @Test
    void specificHostPermission_allowsMatchingHost() {
        PluginAwareHttpClient client = new PluginAwareHttpClient("test-plugin",
                List.of("net:host:api.feishu.cn"));

        assertThatCodeDoesNotThrowSecurity(() -> client.assertNetworkAllowed("https://api.feishu.cn/v1/data"));
    }

    @Test
    void specificHostPermission_blocksNonMatchingHost() {
        PluginAwareHttpClient client = new PluginAwareHttpClient("test-plugin",
                List.of("net:host:api.feishu.cn"));

        assertThatThrownBy(() -> client.get("https://evil.com/steal"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("cannot access host");
    }

    @Test
    void subdomainPermission_allowsSubdomain() {
        PluginAwareHttpClient client = new PluginAwareHttpClient("test-plugin",
                List.of("net:host:example.com"));

        assertThatCodeDoesNotThrowSecurity(() -> client.assertNetworkAllowed("https://api.example.com/data"));
    }

    private void assertThatCodeDoesNotThrowSecurity(Runnable action) {
        try {
            action.run();
        } catch (SecurityException e) {
            throw new AssertionError("Expected no SecurityException but got: " + e.getMessage());
        } catch (Exception e) {
            // Network errors are expected in tests - we only care about SecurityException
        }
    }
}
