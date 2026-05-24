package ai.gameclaw.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
@ConditionalOnProperty(name = "gameclaw.concurrency.pinning-watch.enabled", havingValue = "true", matchIfMissing = true)
public class PinningWatcher {

    private static final Logger log = LoggerFactory.getLogger(PinningWatcher.class);
    private static final long PIN_RATE_THRESHOLD = 10;

    private final AtomicLong pinnedCount = new AtomicLong(0);
    private final AtomicLong lastReportCount = new AtomicLong(0);

    public void recordPinning(String stackTrace) {
        long count = pinnedCount.incrementAndGet();
        log.warn("[PinningWatcher] Virtual thread pinned! Count={}, stack:\n{}", count, stackTrace);
    }

    @Scheduled(fixedRate = 60_000)
    public void reportPinningRate() {
        long current = pinnedCount.get();
        long delta = current - lastReportCount.getAndSet(current);
        if (delta > PIN_RATE_THRESHOLD) {
            log.error("[PinningWatcher] High pinning rate detected: {} pins/min (threshold={})", delta, PIN_RATE_THRESHOLD);
        } else if (delta > 0) {
            log.info("[PinningWatcher] Pinning rate: {} pins/min", delta);
        }
    }

    public long getPinnedCount() {
        return pinnedCount.get();
    }

    public void reset() {
        pinnedCount.set(0);
        lastReportCount.set(0);
    }
}
