package ai.gameclaw.concurrency;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(org.springframework.core.Ordered.HIGHEST_PRECEDENCE)
public class ConcurrencyBanner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ConcurrencyBanner.class);

    @Override
    public void run(ApplicationArguments args) {
        boolean virtualEnabled = Boolean.parseBoolean(
                System.getProperty("spring.threads.virtual.enabled",
                        "true"));

        log.info("[GameClaw] ══════════════════════════════════════════════════════");
        log.info("[GameClaw] Virtual threads: {}", virtualEnabled ? "ENABLED ✅" : "DISABLED ⚠️");
        log.info("[GameClaw] Carrier pool size: {} (availableProcessors)", Runtime.getRuntime().availableProcessors());
        log.info("[GameClaw] JDK version: {}", System.getProperty("java.version"));
        log.info("[GameClaw] JFR pinning trace: add -Djdk.tracePinnedThreads=full to enable");
        log.info("[GameClaw] StructuredTaskScope: GA (JDK 25, no preview flags needed)");
        log.info("[GameClaw] ══════════════════════════════════════════════════════");
    }
}
