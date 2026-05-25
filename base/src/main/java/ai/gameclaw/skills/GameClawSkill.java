package ai.gameclaw.skills;

import java.nio.file.Path;
import java.util.Map;

public record GameClawSkill(
        String name,
        String description,
        String instructions,
        Map<String, Object> metadata,
        boolean userInvocable,
        boolean disableModelInvocation,
        String commandDispatch,
        String commandTool,
        String commandArgMode,
        Path baseDir,
        Map<String, Path> resources
) {
    public String resolveBaseDir(String input) {
        if (input == null || baseDir == null) {
            return input;
        }
        return input.replace("{baseDir}", baseDir.toAbsolutePath().toString());
    }

    public Path resolveResource(String name) {
        if (name == null) return null;
        Path resolved = resources.get(name);
        if (resolved == null) return null;
        if (!resolved.toAbsolutePath().startsWith(baseDir.toAbsolutePath())) {
            return null;
        }
        return resolved;
    }
}
