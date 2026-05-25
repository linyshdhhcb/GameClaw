package ai.gameclaw.skills;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class GameClawSkillsLoaderTest {

    private GameClawSkillParser parser;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        parser = new GameClawSkillParser();
    }

    @Test
    void loadFromWorkspaceDirectory() throws IOException {
        Path wsSkills = tempDir.resolve("workspace").resolve("skills");
        Files.createDirectories(wsSkills);

        Path skillDir = wsSkills.resolve("git");
        Files.createDirectories(skillDir);
        Files.writeString(skillDir.resolve("SKILL.md"), """
                ---
                name: git
                description: Git operations
                ---
                Help with git.
                """);

        GameClawSkillsLoader loader = new GameClawSkillsLoader(parser, "file:" + tempDir.resolve("workspace"));
        var skills = loader.loadAll();

        assertThat(skills).containsKey("git");
        assertThat(skills.get("git").description()).isEqualTo("Git operations");
    }

    @Test
    void workspaceOverridesHome() throws IOException {
        Path homeSkills = tempDir.resolve("home").resolve(".gameclaw").resolve("skills");
        Path wsSkills = tempDir.resolve("workspace").resolve("skills");

        Files.createDirectories(homeSkills);
        Files.createDirectories(wsSkills);

        Path homeSkillDir = homeSkills.resolve("git");
        Files.createDirectories(homeSkillDir);
        Files.writeString(homeSkillDir.resolve("SKILL.md"), """
                ---
                name: git
                description: Home version
                ---
                Home instructions.
                """);

        Path wsSkillDir = wsSkills.resolve("git");
        Files.createDirectories(wsSkillDir);
        Files.writeString(wsSkillDir.resolve("SKILL.md"), """
                ---
                name: git
                description: Workspace version
                ---
                Workspace instructions.
                """);

        GameClawSkillsLoader loader = new GameClawSkillsLoader(parser, "file:" + tempDir.resolve("workspace"));
        loader.loadAll();

        GameClawSkill skill = loader.getLoadedSkill("git");
        assertThat(skill).isNotNull();
        assertThat(skill.description()).isEqualTo("Workspace version");
    }

    @Test
    void loadFromNonexistentDirectoryReturnsEmpty() {
        GameClawSkillsLoader loader = new GameClawSkillsLoader(parser, "file:" + tempDir.resolve("nonexistent"));
        var skills = loader.loadAll();
        assertThat(skills).isEmpty();
    }

    @Test
    void reloadClearsCache() throws IOException {
        Path wsSkills = tempDir.resolve("workspace").resolve("skills");
        Files.createDirectories(wsSkills);

        Path skillDir = wsSkills.resolve("test");
        Files.createDirectories(skillDir);
        Files.writeString(skillDir.resolve("SKILL.md"), """
                ---
                name: test
                description: v1
                ---
                v1 instructions.
                """);

        GameClawSkillsLoader loader = new GameClawSkillsLoader(parser, "file:" + tempDir.resolve("workspace"));
        loader.loadAll();
        assertThat(loader.getLoadedSkill("test").description()).isEqualTo("v1");

        Files.writeString(skillDir.resolve("SKILL.md"), """
                ---
                name: test
                description: v2
                ---
                v2 instructions.
                """);

        loader.reload();
        assertThat(loader.getLoadedSkill("test").description()).isEqualTo("v2");
    }
}
