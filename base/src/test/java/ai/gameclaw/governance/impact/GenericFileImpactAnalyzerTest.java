package ai.gameclaw.governance.impact;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class GenericFileImpactAnalyzerTest {

    @TempDir
    Path tempDir;

    private GenericFileImpactAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new GenericFileImpactAnalyzer();
    }

    @Test
    void supportsNonJavaFiles() {
        assertThat(analyzer.supports(Path.of("Player.cs"))).isTrue();
        assertThat(analyzer.supports(Path.of("scene.gd"))).isTrue();
    }

    @Test
    void doesNotSupportJavaFiles() {
        assertThat(analyzer.supports(Path.of("Main.java"))).isFalse();
    }

    @Test
    void analyzesCSharpReference() throws IOException {
        Path scriptsDir = tempDir.resolve("scripts");
        Files.createDirectories(scriptsDir);

        Files.writeString(scriptsDir.resolve("PlayerController.cs"), """
                public class PlayerController {
                    public void Move() {}
                }
                """);

        Files.writeString(scriptsDir.resolve("GameManager.cs"), """
                public class GameManager {
                    private PlayerController player;
                    public void Update() {
                        player.Move();
                    }
                }
                """);

        ImpactReport report = analyzer.analyze(scriptsDir.resolve("PlayerController.cs"), tempDir);
        assertThat(report.affectedFiles()).isGreaterThan(0);
        assertThat(report.affectedPaths()).anyMatch(p -> p.toString().contains("GameManager"));
    }

    @Test
    void analyzesGdScriptPreload() throws IOException {
        Path scenesDir = tempDir.resolve("scenes");
        Files.createDirectories(scenesDir);

        Files.writeString(scenesDir.resolve("weapon.gd"), """
                extends Node
                func attack():
                    pass
                """);

        Files.writeString(scenesDir.resolve("player.gd"), """
                extends CharacterBody2D
                const Weapon = preload("weapon.gd")
                func _ready():
                    var w = Weapon.new()
                """);

        ImpactReport report = analyzer.analyze(scenesDir.resolve("weapon.gd"), tempDir);
        assertThat(report.affectedFiles()).isGreaterThan(0);
        assertThat(report.affectedPaths()).anyMatch(p -> p.toString().contains("player"));
    }

    @Test
    void returnsEmptyForUnreferencedFile() throws IOException {
        Path scriptsDir = tempDir.resolve("scripts");
        Files.createDirectories(scriptsDir);

        Files.writeString(scriptsDir.resolve("isolated.cs"), """
                public class Isolated {
                    public void Run() {}
                }
                """);

        ImpactReport report = analyzer.analyze(scriptsDir.resolve("isolated.cs"), tempDir);
        assertThat(report.affectedFiles()).isZero();
    }
}
