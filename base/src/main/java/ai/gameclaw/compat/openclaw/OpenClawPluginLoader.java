package ai.gameclaw.compat.openclaw;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

@Component
@ConditionalOnProperty(name = "gameclaw.plugins.enabled", havingValue = "true", matchIfMissing = false)
public class OpenClawPluginLoader {

    private static final Logger log = LoggerFactory.getLogger(OpenClawPluginLoader.class);

    private final PluginManifestReader manifestReader;
    private final Path pluginsDir;
    private final ApplicationEventPublisher eventPublisher;
    private final Map<String, PluginInstance> loadedPlugins = new LinkedHashMap<>();

    public OpenClawPluginLoader(
            ObjectMapper objectMapper,
            ApplicationEventPublisher eventPublisher,
            @Value("${gameclaw.workspace:file:./workspace/}") String workspaceStr) {
        this.manifestReader = new PluginManifestReader(objectMapper);
        this.eventPublisher = eventPublisher;
        String wsPath = workspaceStr.replace("file:", "").replace("file:///", "/");
        this.pluginsDir = Path.of(wsPath).resolve("plugins");
    }

    public synchronized PluginInstance loadFromJar(Path pluginJar) throws PluginLoadException {
        try {
            PluginManifest manifest = manifestReader.readFromJar(pluginJar);
            if (loadedPlugins.containsKey(manifest.name())) {
                throw new PluginLoadException("Plugin already loaded: " + manifest.name());
            }

            PluginClassLoader loader = new PluginClassLoader(pluginJar, getClass().getClassLoader(), manifest.permissions());
            Class<?> mainCls = loader.loadClass(manifest.main());
            Object instance = mainCls.getDeclaredConstructor().newInstance();

            PluginInstance plugin = new PluginInstance(manifest, instance, loader);
            loadedPlugins.put(manifest.name(), plugin);
            eventPublisher.publishEvent(new PluginLoadedEvent(this, plugin));

            log.info("[OpenClawPluginLoader] Loaded plugin: {} v{} ({} tools)",
                    manifest.name(), manifest.version(), plugin.tools().size());
            return plugin;
        } catch (PluginLoadException e) {
            throw e;
        } catch (ClassNotFoundException e) {
            throw new PluginLoadException("Plugin main class not found: " + e.getMessage(), e);
        } catch (NoSuchMethodException e) {
            throw new PluginLoadException("Plugin main class has no default constructor", e);
        } catch (Exception e) {
            throw new PluginLoadException("Failed to load plugin from " + pluginJar, e);
        }
    }

    public synchronized PluginInstance loadFromDirectory(Path pluginDir) throws PluginLoadException {
        try {
            PluginManifest manifest = manifestReader.readFromDirectory(pluginDir);
            if (loadedPlugins.containsKey(manifest.name())) {
                throw new PluginLoadException("Plugin already loaded: " + manifest.name());
            }

            java.net.URL[] urls = new java.net.URL[]{
                    pluginDir.toUri().toURL(),
                    pluginDir.resolve("classes").toUri().toURL()
            };
            PluginClassLoader loader = new PluginClassLoader(urls, getClass().getClassLoader(), manifest.name(), manifest.permissions());
            Class<?> mainCls = loader.loadClass(manifest.main());
            Object instance = mainCls.getDeclaredConstructor().newInstance();

            PluginInstance plugin = new PluginInstance(manifest, instance, loader);
            loadedPlugins.put(manifest.name(), plugin);
            eventPublisher.publishEvent(new PluginLoadedEvent(this, plugin));

            log.info("[OpenClawPluginLoader] Loaded plugin from dir: {} v{} ({} tools)",
                    manifest.name(), manifest.version(), plugin.tools().size());
            return plugin;
        } catch (PluginLoadException e) {
            throw e;
        } catch (ClassNotFoundException e) {
            throw new PluginLoadException("Plugin main class not found: " + e.getMessage(), e);
        } catch (NoSuchMethodException e) {
            throw new PluginLoadException("Plugin main class has no default constructor", e);
        } catch (Exception e) {
            throw new PluginLoadException("Failed to load plugin from " + pluginDir, e);
        }
    }

    public synchronized void loadAllFromPluginsDir() {
        if (!Files.isDirectory(pluginsDir)) {
            log.info("[OpenClawPluginLoader] Plugins directory not found: {}", pluginsDir);
            return;
        }
        try (Stream<Path> walk = Files.walk(pluginsDir, 1)) {
            walk.filter(p -> p != pluginsDir)
                    .filter(p -> Files.isDirectory(p) || p.toString().endsWith(".jar"))
                    .forEach(p -> {
                        try {
                            if (p.toString().endsWith(".jar")) {
                                loadFromJar(p);
                            } else if (Files.exists(p.resolve("plugin.json"))) {
                                loadFromDirectory(p);
                            }
                        } catch (PluginLoadException e) {
                            log.warn("[OpenClawPluginLoader] Failed to load plugin from {}: {}", p, e.getMessage());
                        }
                    });
        } catch (IOException e) {
            log.warn("[OpenClawPluginLoader] Failed to scan plugins directory: {}", e.getMessage());
        }
    }

    public synchronized void unload(String pluginName) {
        PluginInstance plugin = loadedPlugins.remove(pluginName);
        if (plugin != null) {
            plugin.destroy();
            log.info("[OpenClawPluginLoader] Unloaded plugin: {}", pluginName);
        }
    }

    public synchronized PluginInstance getPlugin(String name) {
        return loadedPlugins.get(name);
    }

    public synchronized Map<String, PluginInstance> getAllPlugins() {
        return Collections.unmodifiableMap(loadedPlugins);
    }

    public Path pluginsDir() {
        return pluginsDir;
    }
}
