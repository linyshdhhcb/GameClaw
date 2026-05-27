package ai.gameclaw.compat.openclaw;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class PluginInstance {

    private final PluginManifest manifest;
    private final Object pluginInstance;
    private final PluginClassLoader classLoader;
    private final List<OpenClawTool> tools;

    public PluginInstance(PluginManifest manifest, Object pluginInstance, PluginClassLoader classLoader) {
        this.manifest = manifest;
        this.pluginInstance = pluginInstance;
        this.classLoader = classLoader;
        this.tools = discoverTools();
    }

    public String name() {
        return manifest.name();
    }

    public String version() {
        return manifest.version();
    }

    public PluginManifest manifest() {
        return manifest;
    }

    public Object pluginObject() {
        return pluginInstance;
    }

    public PluginClassLoader classLoader() {
        return classLoader;
    }

    public List<OpenClawTool> tools() {
        return tools;
    }

    public void destroy() {
        try {
            Method close = pluginInstance.getClass().getMethod("close");
            close.invoke(pluginInstance);
        } catch (NoSuchMethodException ignored) {
        } catch (Exception e) {
            throw new RuntimeException("Failed to destroy plugin: " + manifest.name(), e);
        }
    }

    private List<OpenClawTool> discoverTools() {
        List<OpenClawTool> found = new ArrayList<>();
        if (pluginInstance instanceof OpenClawTool tool) {
            found.add(tool);
        }
        for (Class<?> iface : pluginInstance.getClass().getInterfaces()) {
            if (iface.equals(OpenClawTool.class)) continue;
            if (OpenClawTool.class.isAssignableFrom(iface)) {
                try {
                    found.add((OpenClawTool) pluginInstance);
                } catch (ClassCastException ignored) {}
            }
        }
        for (String toolName : manifest.declaredTools()) {
            try {
                String toolClassName = manifest.main().substring(0, manifest.main().lastIndexOf('.') + 1) + toPascalCase(toolName);
                Class<?> toolCls = classLoader.loadClass(toolClassName);
                if (OpenClawTool.class.isAssignableFrom(toolCls)) {
                    found.add((OpenClawTool) toolCls.getDeclaredConstructor().newInstance());
                }
            } catch (Exception ignored) {}
        }
        return found;
    }

    private String toPascalCase(String snakeOrKebab) {
        StringBuilder sb = new StringBuilder();
        boolean capitalizeNext = true;
        for (char c : snakeOrKebab.toCharArray()) {
            if (c == '_' || c == '-') {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                sb.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
