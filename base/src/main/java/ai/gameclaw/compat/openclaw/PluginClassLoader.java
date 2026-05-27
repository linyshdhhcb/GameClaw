package ai.gameclaw.compat.openclaw;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

public class PluginClassLoader extends URLClassLoader {

    private static final Logger log = LoggerFactory.getLogger(PluginClassLoader.class);

    private static final Set<String> ALLOWED_GAMECLAW_PREFIXES = Set.of(
            "ai.gameclaw.compat.openclaw.",
            "io.openclaw.api."
    );

    private final String pluginName;
    private final List<String> permissions;

    public PluginClassLoader(Path pluginJar, ClassLoader parent, List<String> permissions) {
        super("plugin-" + pluginJar.getFileName(), toUrls(pluginJar), parent);
        this.pluginName = pluginJar.getFileName().toString();
        this.permissions = permissions != null ? permissions : List.of();
    }

    public PluginClassLoader(URL[] urls, ClassLoader parent, String pluginName, List<String> permissions) {
        super("plugin-" + pluginName, urls, parent);
        this.pluginName = pluginName;
        this.permissions = permissions != null ? permissions : List.of();
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (isBlockedInternalClass(name)) {
            log.warn("[PluginClassLoader] Blocked access to internal class: {} (plugin={})", name, pluginName);
            throw new ClassNotFoundException("Plugin cannot access internal class: " + name);
        }
        return super.loadClass(name, resolve);
    }

    @Override
    public URL getResource(String name) {
        URL url = findResource(name);
        if (url != null) return url;
        return super.getResource(name);
    }

    public boolean hasPermission(String permission) {
        if (permissions.isEmpty()) return false;
        if (permissions.contains("*")) return true;
        return permissions.stream().anyMatch(p -> permission.startsWith(p) || p.equals(permission));
    }

    public String pluginName() {
        return pluginName;
    }

    public List<String> permissions() {
        return permissions;
    }

    private boolean isBlockedInternalClass(String name) {
        if (!name.startsWith("ai.gameclaw.")) return false;
        for (String allowed : ALLOWED_GAMECLAW_PREFIXES) {
            if (name.startsWith(allowed)) return false;
        }
        return true;
    }

    private static URL[] toUrls(Path jar) {
        try {
            return new URL[]{jar.toUri().toURL()};
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid plugin jar path: " + jar, e);
        }
    }
}
