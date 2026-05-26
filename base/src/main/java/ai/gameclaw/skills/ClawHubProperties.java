package ai.gameclaw.skills;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gameclaw.clawhub")
public record ClawHubProperties(
        String registryUrl,
        Boolean enabled,
        String installDir
) {

    public ClawHubProperties {
        if (registryUrl == null) registryUrl = "https://registry.clawhub.io";
        if (enabled == null) enabled = false;
        if (installDir == null) installDir = System.getProperty("user.home") + "/.gameclaw/skills";
    }
}
