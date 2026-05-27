package ai.gameclaw.compat.openclaw;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PluginManifest(
        String name,
        String version,
        String main,
        List<String> permissions,
        @JsonProperty("tools") List<String> declaredTools,
        @JsonProperty("minOpenClawVersion") String minOpenClawVersion,
        @JsonProperty("description") String description
) {
    public PluginManifest {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("plugin.json 'name' is required");
        }
        if (version == null || version.isBlank()) {
            throw new IllegalArgumentException("plugin.json 'version' is required");
        }
        if (main == null || main.isBlank()) {
            throw new IllegalArgumentException("plugin.json 'main' is required");
        }
        if (permissions == null) {
            permissions = List.of();
        }
        if (declaredTools == null) {
            declaredTools = List.of();
        }
    }
}
