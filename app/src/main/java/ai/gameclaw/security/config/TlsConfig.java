package ai.gameclaw.security.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "server.ssl.enabled", havingValue = "true")
public class TlsConfig {

    private static final Logger log = LoggerFactory.getLogger(TlsConfig.class);

    @EventListener(ApplicationReadyEvent.class)
    public void logTlsStatus() {
        log.info("[GameClaw] TLS enabled: server.ssl.enabled=true");
        log.info("[GameClaw] Ensure cert-manager is configured in production");
    }
}
