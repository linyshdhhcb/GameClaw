package ai.gameclaw.compat.openclaw;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public final class PluginFs {

    private static final Logger log = LoggerFactory.getLogger(PluginFs.class);

    private PluginFs() {}

    public static Path resolveSafe(Path workspace, PluginInstance plugin, String relative) {
        Path root = workspace.resolve("plugins").resolve(plugin.name()).normalize();
        Path resolved = root.resolve(relative).normalize();
        if (!resolved.startsWith(root)) {
            log.warn("[PluginFs] Path escape blocked: {} (plugin={})", relative, plugin.name());
            throw new SecurityException("Plugin path escape: " + relative);
        }
        return resolved;
    }

    public static Path resolveSafe(Path workspace, String pluginName, String relative) {
        Path root = workspace.resolve("plugins").resolve(pluginName).normalize();
        Path resolved = root.resolve(relative).normalize();
        if (!resolved.startsWith(root)) {
            log.warn("[PluginFs] Path escape blocked: {} (plugin={})", relative, pluginName);
            throw new SecurityException("Plugin path escape: " + relative);
        }
        return resolved;
    }

    public static Path pluginRoot(Path workspace, String pluginName) {
        return workspace.resolve("plugins").resolve(pluginName).normalize();
    }

    public static boolean isWithinPlugin(Path workspace, String pluginName, Path path) {
        Path root = workspace.resolve("plugins").resolve(pluginName).normalize();
        Path normalized = path.normalize();
        return normalized.startsWith(root);
    }
}
