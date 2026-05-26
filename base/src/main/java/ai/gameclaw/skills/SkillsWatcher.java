package ai.gameclaw.skills;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

@Component
public class SkillsWatcher {

    private static final Logger log = LoggerFactory.getLogger(SkillsWatcher.class);
    private static final long DEBOUNCE_MS = 250L;

    private final GameClawSkillsLoader skillsLoader;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectProvider<MeterRegistry> meterRegistryProvider;
    private final String workspaceStr;
    private final long pollingIntervalMs;

    private final Map<WatchKey, Path> watchedKeys = new ConcurrentHashMap<>();
    private final AtomicBoolean debounceScheduled = new AtomicBoolean(false);
    private volatile ScheduledExecutorService scheduler;
    private volatile WatchService watchService;
    private volatile boolean running = false;

    public SkillsWatcher(
            GameClawSkillsLoader skillsLoader,
            ApplicationEventPublisher eventPublisher,
            ObjectProvider<MeterRegistry> meterRegistryProvider,
            @Value("${gameclaw.workspace:file:./workspace/}") String workspaceStr,
            @Value("${gameclaw.skills.polling-interval:0}") long pollingIntervalMs) {
        this.skillsLoader = skillsLoader;
        this.eventPublisher = eventPublisher;
        this.meterRegistryProvider = meterRegistryProvider;
        this.workspaceStr = workspaceStr;
        this.pollingIntervalMs = pollingIntervalMs;
    }

    @PostConstruct
    void startWatching() {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "skills-watcher-scheduler");
            t.setDaemon(true);
            return t;
        });
        running = true;

        if (pollingIntervalMs > 0) {
            log.info("[SkillsWatcher] Polling mode enabled with interval {}ms", pollingIntervalMs);
            scheduler.scheduleWithFixedDelay(
                    () -> triggerReload("poll"),
                    pollingIntervalMs, pollingIntervalMs, TimeUnit.MILLISECONDS);
        } else {
            startWatchService();
        }
    }

    @PreDestroy
    void stopWatching() {
        running = false;
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException e) {
                log.warn("[SkillsWatcher] Error closing WatchService: {}", e.getMessage());
            }
        }
    }

    private void startWatchService() {
        try {
            watchService = FileSystems.getDefault().newWatchService();

            List<Path> watchDirs = List.of(
                    Path.of(System.getProperty("user.home"), ".openclaw", "skills"),
                    Path.of(System.getProperty("user.home"), ".gameclaw", "skills"),
                    resolveWorkspaceSkills()
            );

            for (Path dir : watchDirs) {
                registerDirectoryTree(dir);
            }

            Thread.ofVirtual().name("skills-watcher").start(this::watchLoop);

            log.info("[SkillsWatcher] Started watching {} directories", watchedKeys.size());
        } catch (IOException e) {
            log.error("[SkillsWatcher] Failed to start WatchService: {}", e.getMessage());
        }
    }

    private Path resolveWorkspaceSkills() {
        String wsPath = workspaceStr.replace("file:", "").replace("file:///", "/");
        return Path.of(wsPath).resolve("skills");
    }

    private void registerDirectoryTree(Path root) {
        if (!Files.isDirectory(root)) {
            log.debug("[SkillsWatcher] Directory does not exist, skipping: {}", root);
            return;
        }
        registerSingleDirectory(root);
        try (Stream<Path> walk = Files.walk(root)) {
            walk.filter(Files::isDirectory)
                    .forEach(this::registerSingleDirectory);
        } catch (IOException e) {
            log.warn("[SkillsWatcher] Failed to walk directory tree {}: {}", root, e.getMessage());
        }
    }

    private void registerSingleDirectory(Path dir) {
        try {
            WatchKey key = dir.register(watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE,
                    StandardWatchEventKinds.ENTRY_MODIFY);
            watchedKeys.put(key, dir);
            log.debug("[SkillsWatcher] Watching: {}", dir.toAbsolutePath());
        } catch (IOException e) {
            log.warn("[SkillsWatcher] Cannot register {}: {}", dir, e.getMessage());
        }
    }

    private void watchLoop() {
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                WatchKey key = watchService.take();
                List<WatchEvent<?>> events = key.pollEvents();

                for (WatchEvent<?> event : events) {
                    if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE
                            && event.context() instanceof Path createdPath) {
                        Path dir = watchedKeys.get(key);
                        if (dir != null) {
                            Path created = dir.resolve(createdPath);
                            if (Files.isDirectory(created)) {
                                registerDirectoryTree(created);
                            }
                        }
                    }
                }

                boolean hasRelevantChange = events.stream()
                        .anyMatch(e -> !(e.context() instanceof Path path) ||
                                !path.toString().startsWith("."));

                key.reset();

                if (hasRelevantChange) {
                    scheduleDebouncedReload();
                }
            } catch (ClosedWatchServiceException | InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private synchronized void scheduleDebouncedReload() {
        if (scheduler == null) return;
        if (!debounceScheduled.compareAndSet(false, true)) return;

        scheduler.schedule(() -> {
            debounceScheduled.set(false);
            triggerReload("watch");
        }, DEBOUNCE_MS, TimeUnit.MILLISECONDS);
    }

    private void triggerReload(String source) {
        try {
            log.info("[SkillsWatcher] Reloading skills (triggered by {})", source);
            skillsLoader.reload();
            eventPublisher.publishEvent(new SkillsChangedEvent(this));
            recordMetrics();
            log.info("[SkillsWatcher] Skill reload complete and event published");
        } catch (Exception e) {
            log.error("[SkillsWatcher] Failed to reload skills: {}", e.getMessage());
        }
    }

    private void recordMetrics() {
        MeterRegistry registry = meterRegistryProvider.getIfAvailable();
        if (registry == null) return;
        Counter.builder("skills_reload_total")
                .register(registry)
                .increment();
    }
}
