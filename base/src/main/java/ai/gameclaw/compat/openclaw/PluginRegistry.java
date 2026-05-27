package ai.gameclaw.compat.openclaw;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(name = "gameclaw.plugins.enabled", havingValue = "true", matchIfMissing = false)
public class PluginRegistry {

    private static final Logger log = LoggerFactory.getLogger(PluginRegistry.class);

    private final OpenClawPluginLoader pluginLoader;

    public PluginRegistry(OpenClawPluginLoader pluginLoader) {
        this.pluginLoader = pluginLoader;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("[PluginRegistry] Auto-loading plugins from: {}", pluginLoader.pluginsDir());
        pluginLoader.loadAllFromPluginsDir();
        log.info("[PluginRegistry] {} plugin(s) loaded", pluginLoader.getAllPlugins().size());
    }

    public List<OpenClawTool> getAllTools() {
        return pluginLoader.getAllPlugins().values().stream()
                .flatMap(p -> p.tools().stream())
                .toList();
    }

    public List<OpenClawTool> getToolsForPlugin(String pluginName) {
        PluginInstance plugin = pluginLoader.getPlugin(pluginName);
        return plugin != null ? plugin.tools() : List.of();
    }

    public OpenClawPluginLoader loader() {
        return pluginLoader;
    }
}
