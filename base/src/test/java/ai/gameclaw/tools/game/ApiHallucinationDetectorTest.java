package ai.gameclaw.tools.game;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ApiHallucinationDetectorTest {

    @TempDir
    Path tempDir;

    private ApiHallucinationDetector detector;

    @BeforeEach
    void setUp() throws IOException {
        Path workspace = tempDir.resolve("workspace");
        Files.createDirectories(workspace);
        Path skillsDir = workspace.resolve("game-skills");

        Path unityDir = skillsDir.resolve("unity");
        Files.createDirectories(unityDir);
        Files.writeString(unityDir.resolve("api-index.json"), """
                {"engine":"unity","apis":[
                    {"fqn":"UnityEngine.GameObject","kind":"class","since":"1.0"},
                    {"fqn":"UnityEngine.GameObject.Find","kind":"method","since":"1.0"},
                    {"fqn":"UnityEngine.Debug","kind":"class","since":"1.0"},
                    {"fqn":"UnityEngine.Debug.Log","kind":"method","since":"1.0"}
                ]}
                """);

        Path godotDir = skillsDir.resolve("godot");
        Files.createDirectories(godotDir);
        Files.writeString(godotDir.resolve("api-index.json"), """
                {"engine":"godot","apis":[
                    {"fqn":"get_node","kind":"method","since":"1.0"},
                    {"fqn":"queue_free","kind":"method","since":"1.0"}
                ]}
                """);

        detector = new ApiHallucinationDetector(
                new org.springframework.core.io.FileSystemResource(workspace.toFile()), null);
    }

    @Test
    void detectUnityKnownApi() {
        var result = detector.detect(Engine.UNITY, "UnityEngine.GameObject.Find(\"test\"); UnityEngine.Debug.Log(\"hi\");");
        assertThat(result.unknownApis()).isEmpty();
    }

    @Test
    void detectUnityUnknownApi() {
        var result = detector.detect(Engine.UNITY, "UnityEngine.FakeClass.NonExistentMethod();");
        assertThat(result.unknownApis()).isNotEmpty();
    }

    @Test
    void detectReturnsTotalApiCount() {
        var result = detector.detect(Engine.UNITY, "UnityEngine.GameObject.Find(\"test\");");
        assertThat(result.totalApis()).isGreaterThan(0);
    }

    @Test
    void detectEmptyCode() {
        var result = detector.detect(Engine.UNITY, "");
        assertThat(result.unknownApis()).isEmpty();
        assertThat(result.totalApis()).isEqualTo(0);
    }

    @Test
    void detectGodotKnownApi() {
        var result = detector.detect(Engine.GODOT, "get_node(\"Player\"); queue_free();");
        assertThat(result.unknownApis()).isEmpty();
    }
}
