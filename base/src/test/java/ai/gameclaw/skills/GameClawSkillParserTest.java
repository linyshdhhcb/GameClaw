package ai.gameclaw.skills;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GameClawSkillParserTest {

    private GameClawSkillParser parser;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        parser = new GameClawSkillParser();
    }

    @Test
    void parseValidSkillMd() throws IOException {
        Path skillMd = tempDir.resolve("SKILL.md");
        Files.writeString(skillMd, """
                ---
                name: git
                description: Git version control operations
                user-invocable: true
                command-dispatch: tool
                command-tool: git_tool
                ---
                You are a Git expert. Help users with version control tasks.
                """);

        GameClawSkill skill = parser.parse(skillMd);

        assertThat(skill).isNotNull();
        assertThat(skill.name()).isEqualTo("git");
        assertThat(skill.description()).isEqualTo("Git version control operations");
        assertThat(skill.instructions()).contains("You are a Git expert");
        assertThat(skill.userInvocable()).isTrue();
        assertThat(skill.commandDispatch()).isEqualTo("tool");
        assertThat(skill.commandTool()).isEqualTo("git_tool");
        assertThat(skill.baseDir()).isEqualTo(tempDir);
    }

    @Test
    void parseWithMetadata() throws IOException {
        Path skillMd = tempDir.resolve("SKILL.md");
        Files.writeString(skillMd, """
                ---
                name: code-review
                description: Code review assistant
                metadata:
                  gate: strict
                  version: "1.0"
                ---
                Review code for quality issues.
                """);

        GameClawSkill skill = parser.parse(skillMd);

        assertThat(skill).isNotNull();
        assertThat(skill.name()).isEqualTo("code-review");
        assertThat(skill.metadata()).isNotNull();
        assertThat(skill.metadata()).containsEntry("gate", "strict");
        assertThat(skill.metadata()).containsEntry("version", "1.0");
    }

    @Test
    void parseMissingNameReturnsNull() throws IOException {
        Path skillMd = tempDir.resolve("SKILL.md");
        Files.writeString(skillMd, """
                ---
                description: No name skill
                ---
                Some instructions.
                """);

        GameClawSkill skill = parser.parse(skillMd);

        assertThat(skill).isNull();
    }

    @Test
    void parseNoFrontmatter() throws IOException {
        Path skillMd = tempDir.resolve("SKILL.md");
        Files.writeString(skillMd, "Just plain markdown content without frontmatter.");

        GameClawSkill skill = parser.parse(skillMd);

        assertThat(skill).isNull();
    }

    @Test
    void resolveBaseDirPlaceholder() throws IOException {
        Path skillMd = tempDir.resolve("SKILL.md");
        Files.writeString(skillMd, """
                ---
                name: test
                description: test
                ---
                Instructions
                """);

        GameClawSkill skill = parser.parse(skillMd);
        String resolved = skill.resolveBaseDir("Read file at {baseDir}/config.yaml");
        assertThat(resolved).contains(tempDir.toAbsolutePath().toString());
        assertThat(resolved).doesNotContain("{baseDir}");
    }

    @Test
    void resolveResourcePreventsPathTraversal() throws IOException {
        Path skillMd = tempDir.resolve("SKILL.md");
        Files.writeString(skillMd, """
                ---
                name: test
                description: test
                ---
                Instructions
                """);

        GameClawSkill skill = parser.parse(skillMd);
        assertThat(skill.resolveResource("nonexistent")).isNull();
    }

    @Test
    void indexResources() throws IOException {
        Files.writeString(tempDir.resolve("SKILL.md"), """
                ---
                name: test
                description: test
                ---
                Instructions
                """);
        Files.writeString(tempDir.resolve("config.yaml"), "key: value");
        Files.writeString(tempDir.resolve("template.txt"), "hello");

        GameClawSkill skill = parser.parse(tempDir.resolve("SKILL.md"));
        assertThat(skill.resources()).containsKey("config.yaml");
        assertThat(skill.resources()).containsKey("template.txt");
        assertThat(skill.resources()).doesNotContainKey("SKILL.md");
    }
}
