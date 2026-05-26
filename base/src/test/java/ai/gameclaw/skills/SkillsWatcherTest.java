package ai.gameclaw.skills;

import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class SkillsWatcherTest {

    @TempDir
    Path tempDir;

    private GameClawSkillParser parser;

    @BeforeEach
    void setUp() {
        parser = new GameClawSkillParser();
    }

    @Test
    void fileChangeTriggersReload() throws Exception {
        Path wsSkills = tempDir.resolve("workspace").resolve("skills");
        Files.createDirectories(wsSkills);

        Path skillDir = wsSkills.resolve("test-skill");
        Files.createDirectories(skillDir);
        Path skillMd = skillDir.resolve("SKILL.md");
        Files.writeString(skillMd, """
                ---
                name: test-skill
                description: v1
                ---
                v1 instructions.
                """);

        GameClawSkillsLoader realLoader = new GameClawSkillsLoader(parser, "file:" + tempDir.resolve("workspace"));
        realLoader.loadAll();

        AtomicInteger events = new AtomicInteger(0);
        ApplicationEventPublisher publisher = e -> {
            if (e instanceof SkillsChangedEvent) {
                events.incrementAndGet();
            }
        };

        SkillsWatcher watcher = new SkillsWatcher(realLoader, publisher,
                emptyMeterRegistry(), "file:" + tempDir.resolve("workspace"), 0);
        watcher.startWatching();

        try {
            Thread.sleep(1000);

            Files.writeString(skillMd, """
                    ---
                    name: test-skill
                    description: v2
                    ---
                    v2 instructions.
                    """, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            boolean sawEvent = awaitCondition(() -> events.get() >= 1, 10);
            assertThat(sawEvent).isTrue();
            assertThat(realLoader.getLoadedSkill("test-skill").description()).isEqualTo("v2");
        } finally {
            watcher.stopWatching();
        }
    }

    @Test
    void debounceMultipleRapidChangesOnlyTriggersOneReload() throws Exception {
        Path wsSkills = tempDir.resolve("workspace").resolve("skills");
        Files.createDirectories(wsSkills);

        Path skillDir = wsSkills.resolve("debounce-skill");
        Files.createDirectories(skillDir);
        Path skillMd = skillDir.resolve("SKILL.md");
        Files.writeString(skillMd, """
                ---
                name: debounce-skill
                description: v1
                ---
                v1.
                """);

        GameClawSkillsLoader realLoader = new GameClawSkillsLoader(parser, "file:" + tempDir.resolve("workspace"));
        realLoader.loadAll();

        AtomicInteger events = new AtomicInteger(0);
        ApplicationEventPublisher publisher = e -> {
            if (e instanceof SkillsChangedEvent) {
                events.incrementAndGet();
            }
        };

        SkillsWatcher watcher = new SkillsWatcher(realLoader, publisher,
                emptyMeterRegistry(), "file:" + tempDir.resolve("workspace"), 0);
        watcher.startWatching();

        try {
            Thread.sleep(1000);

            int initialEvents = events.get();

            for (int i = 0; i < 5; i++) {
                Files.writeString(skillMd, """
                        ---
                        name: debounce-skill
                        description: rapid-%d
                        ---
                        rapid.
                        """.formatted(i), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                Thread.sleep(50);
            }

            Thread.sleep(2000);

            int eventsAfterBurst = events.get();
            int newEvents = eventsAfterBurst - initialEvents;
            assertThat(newEvents).isBetween(1, 3);
        } finally {
            watcher.stopWatching();
        }
    }

    @Test
    void skillsChangedEventIsPublishedOnReload() throws Exception {
        Path wsSkills = tempDir.resolve("workspace").resolve("skills");
        Files.createDirectories(wsSkills);

        Path skillDir = wsSkills.resolve("event-skill");
        Files.createDirectories(skillDir);
        Path skillMd = skillDir.resolve("SKILL.md");
        Files.writeString(skillMd, """
                ---
                name: event-skill
                description: v1
                ---
                v1.
                """);

        GameClawSkillsLoader realLoader = new GameClawSkillsLoader(parser, "file:" + tempDir.resolve("workspace"));
        realLoader.loadAll();

        CountDownLatch eventLatch = new CountDownLatch(1);
        ApplicationEventPublisher publisher = e -> {
            if (e instanceof SkillsChangedEvent) {
                eventLatch.countDown();
            }
        };

        SkillsWatcher watcher = new SkillsWatcher(realLoader, publisher,
                emptyMeterRegistry(), "file:" + tempDir.resolve("workspace"), 0);
        watcher.startWatching();

        try {
            Thread.sleep(1000);

            Files.writeString(skillMd, """
                    ---
                    name: event-skill
                    description: v2
                    ---
                    v2.
                    """, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            boolean received = eventLatch.await(10, TimeUnit.SECONDS);
            assertThat(received).isTrue();
        } finally {
            watcher.stopWatching();
        }
    }

    private ObjectProvider<MeterRegistry> emptyMeterRegistry() {
        return new ObjectProvider<>() {
            @Override public MeterRegistry getObject() { return null; }
            @Override public MeterRegistry getIfAvailable() { return null; }
            @Override public MeterRegistry getIfUnique() { return null; }
        };
    }

    private boolean awaitCondition(java.util.function.Supplier<Boolean> condition, int timeoutSeconds)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
        while (System.currentTimeMillis() < deadline) {
            if (condition.get()) return true;
            Thread.sleep(100);
        }
        return condition.get();
    }
}
