package ai.gameclaw.security.net;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

@Component
@ConfigurationProperties(prefix = "gameclaw.security.outbound")
public class OutboundUrlFilter {

    private static final Logger log = LoggerFactory.getLogger(OutboundUrlFilter.class);

    private Set<String> allowedHosts = new HashSet<>();
    private boolean enabled = true;

    public Set<String> getAllowedHosts() {
        return allowedHosts;
    }

    public void setAllowedHosts(Set<String> allowedHosts) {
        this.allowedHosts = allowedHosts;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isAllowed(URI uri) {
        if (!enabled) {
            return true;
        }
        String host = uri.getHost();
        boolean allowed = allowedHosts.contains(host);
        if (!allowed) {
            log.warn("[OutboundUrlFilter] Blocked outbound request to: {}", uri);
        }
        return allowed;
    }

    public void assertAllowed(URI uri) {
        if (!isAllowed(uri)) {
            throw new SecurityException("Outbound request blocked: " + uri);
        }
    }
}
