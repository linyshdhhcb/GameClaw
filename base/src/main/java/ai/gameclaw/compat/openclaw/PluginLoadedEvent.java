package ai.gameclaw.compat.openclaw;

import org.springframework.context.ApplicationEvent;

public class PluginLoadedEvent extends ApplicationEvent {

    private final PluginInstance plugin;

    public PluginLoadedEvent(Object source, PluginInstance plugin) {
        super(source);
        this.plugin = plugin;
    }

    public PluginInstance plugin() {
        return plugin;
    }
}
