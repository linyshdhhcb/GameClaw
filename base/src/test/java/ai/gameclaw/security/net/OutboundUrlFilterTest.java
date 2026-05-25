package ai.gameclaw.security.net;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OutboundUrlFilterTest {

    @Test
    void allowsWhitelistedHost() {
        OutboundUrlFilter filter = new OutboundUrlFilter();
        filter.setEnabled(true);
        filter.setAllowedHosts(java.util.Set.of("api.anthropic.com", "api.openai.com"));

        assertThat(filter.isAllowed(URI.create("https://api.anthropic.com/v1/messages"))).isTrue();
    }

    @Test
    void blocksNonWhitelistedHost() {
        OutboundUrlFilter filter = new OutboundUrlFilter();
        filter.setEnabled(true);
        filter.setAllowedHosts(java.util.Set.of("api.anthropic.com"));

        assertThat(filter.isAllowed(URI.create("https://evil.example.com/steal"))).isFalse();
    }

    @Test
    void assertAllowedThrowsOnBlockedHost() {
        OutboundUrlFilter filter = new OutboundUrlFilter();
        filter.setEnabled(true);
        filter.setAllowedHosts(java.util.Set.of("api.anthropic.com"));

        assertThatThrownBy(() -> filter.assertAllowed(URI.create("https://evil.example.com/steal")))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Outbound request blocked");
    }

    @Test
    void allowsAllWhenDisabled() {
        OutboundUrlFilter filter = new OutboundUrlFilter();
        filter.setEnabled(false);

        assertThat(filter.isAllowed(URI.create("https://evil.example.com/steal"))).isTrue();
    }
}
