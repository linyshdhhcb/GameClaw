package ai.gameclaw.tools.sandbox;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SandboxWriterTest {

    @TempDir
    Path tempDir;

    private SandboxWriter sandboxWriter;

    @BeforeEach
    void setUp() throws IOException {
        Path workspace = tempDir.resolve("workspace");
        Files.createDirectories(workspace);
        sandboxWriter = new SandboxWriter(new org.springframework.core.io.FileSystemResource(workspace.toFile()));
    }

    @Test
    void writeCreatesFile() throws IOException {
        Path file = sandboxWriter.write(null, Path.of("output/configs/test.json"), "hello".getBytes());
        assertThat(Files.exists(file)).isTrue();
        assertThat(Files.readString(file)).isEqualTo("hello");
    }

    @Test
    void writeCreatesParentDirectories() throws IOException {
        Path file = sandboxWriter.write(null, Path.of("output/deep/nested/test.json"), "data".getBytes());
        assertThat(Files.exists(file)).isTrue();
    }

    @Test
    void writeRejectsPathTraversal() {
        assertThatThrownBy(() ->
                sandboxWriter.write(null, Path.of("../../etc/passwd"), "hack".getBytes())
        ).isInstanceOf(SecurityException.class)
                .hasMessageContaining("Path traversal");
    }

    @Test
    void writeRejectsAbsolutePath() {
        assertThatThrownBy(() ->
                sandboxWriter.write(null, Path.of("/etc/passwd"), "hack".getBytes())
        ).isInstanceOf(SecurityException.class);
    }

    @Test
    void readReturnsWrittenContent() throws IOException {
        sandboxWriter.write(null, Path.of("output/test.json"), "content".getBytes());
        byte[] read = sandboxWriter.read(null, Path.of("output/test.json"));
        assertThat(new String(read)).isEqualTo("content");
    }

    @Test
    void existsReturnsCorrectly() throws IOException {
        assertThat(sandboxWriter.exists(null, Path.of("output/test.json"))).isFalse();
        sandboxWriter.write(null, Path.of("output/test.json"), "data".getBytes());
        assertThat(sandboxWriter.exists(null, Path.of("output/test.json"))).isTrue();
    }

    @Test
    void deleteRemovesFile() throws IOException {
        sandboxWriter.write(null, Path.of("output/test.json"), "data".getBytes());
        sandboxWriter.delete(null, Path.of("output/test.json"));
        assertThat(sandboxWriter.exists(null, Path.of("output/test.json"))).isFalse();
    }
}
