package ai.gameclaw.governance.impact;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class JavaImpactAnalyzerTest {

    @TempDir
    Path tempDir;

    private JavaImpactAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new JavaImpactAnalyzer();
    }

    @Test
    void supportsJavaFiles() {
        assertThat(analyzer.supports(Path.of("MyClass.java"))).isTrue();
    }

    @Test
    void doesNotSupportNonJavaFiles() {
        assertThat(analyzer.supports(Path.of("MyClass.cs"))).isFalse();
    }

    @Test
    void analyzesSimpleClassUsage() throws IOException {
        Path srcDir = tempDir.resolve("src");
        Files.createDirectories(srcDir);

        Files.writeString(srcDir.resolve("TargetClass.java"), """
                public class TargetClass {
                    public void doWork() {}
                }
                """);

        Files.writeString(srcDir.resolve("CallerClass.java"), """
                public class CallerClass {
                    private TargetClass target;
                    public void run() {
                        target.doWork();
                    }
                }
                """);

        ImpactReport report = analyzer.analyze(srcDir.resolve("TargetClass.java"), tempDir);
        assertThat(report.affectedFiles()).isGreaterThan(0);
        assertThat(report.affectedPaths()).anyMatch(p -> p.toString().contains("CallerClass"));
    }

    @Test
    void analyzesInterfaceImplementation() throws IOException {
        Path srcDir = tempDir.resolve("src");
        Files.createDirectories(srcDir);

        Files.writeString(srcDir.resolve("GameService.java"), """
                public interface GameService {
                    void start();
                }
                """);

        Files.writeString(srcDir.resolve("GameServiceImpl.java"), """
                public class GameServiceImpl implements GameService {
                    @Override
                    public void start() {}
                }
                """);

        ImpactReport report = analyzer.analyze(srcDir.resolve("GameService.java"), tempDir);
        assertThat(report.affectedFiles()).isGreaterThan(0);
        assertThat(report.affectedPaths()).anyMatch(p -> p.toString().contains("GameServiceImpl"));
    }

    @Test
    void returnsEmptyForUnreferencedFile() throws IOException {
        Path srcDir = tempDir.resolve("src");
        Files.createDirectories(srcDir);

        Files.writeString(srcDir.resolve("StandaloneClass.java"), """
                public class StandaloneClass {
                    public void execute() {}
                }
                """);

        ImpactReport report = analyzer.analyze(srcDir.resolve("StandaloneClass.java"), tempDir);
        assertThat(report.affectedFiles()).isZero();
    }

    @Test
    void excludesTargetDirectories() throws IOException {
        Path srcDir = tempDir.resolve("src");
        Path targetDir = tempDir.resolve("target");
        Files.createDirectories(srcDir);
        Files.createDirectories(targetDir);

        Files.writeString(srcDir.resolve("CoreService.java"), """
                public class CoreService {
                    public void process() {}
                }
                """);

        Files.writeString(targetDir.resolve("CoreServiceTest.java"), """
                public class CoreServiceTest {
                    private CoreService service;
                }
                """);

        ImpactReport report = analyzer.analyze(srcDir.resolve("CoreService.java"), tempDir);
        assertThat(report.affectedPaths()).noneMatch(p -> p.toString().contains("target"));
    }
}
