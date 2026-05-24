package ai.gameclaw.compat;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class ConfigPathMapper {

    private static final Map<String, String> OPENCLAW_TO_GAMECLAW = new LinkedHashMap<>();

    static {
        OPENCLAW_TO_GAMECLAW.put("channels.", "agent.channels.");
        OPENCLAW_TO_GAMECLAW.put("ai.model", "spring.ai.model.chat");
        OPENCLAW_TO_GAMECLAW.put("ai.", "spring.ai.");
        OPENCLAW_TO_GAMECLAW.put("skills.", "agent.skills.");
        OPENCLAW_TO_GAMECLAW.put("mcp.", "spring.ai.mcp.client.");
    }

    public String mapKey(String openClawKey) {
        for (Map.Entry<String, String> entry : OPENCLAW_TO_GAMECLAW.entrySet()) {
            if (openClawKey.startsWith(entry.getKey())) {
                return entry.getValue() + openClawKey.substring(entry.getKey().length());
            }
        }
        return openClawKey;
    }

    public Map<String, Object> mapKeys(Map<String, Object> openClawConfig) {
        Map<String, Object> result = new LinkedHashMap<>();
        openClawConfig.forEach((key, value) -> {
            String mappedKey = mapKey(key);
            if (value instanceof Map nested) {
                result.put(mappedKey, mapKeys((Map<String, Object>) nested));
            } else {
                result.put(mappedKey, value);
            }
        });
        return result;
    }

    public String reverseMapKey(String gameClawKey) {
        for (Map.Entry<String, String> entry : OPENCLAW_TO_GAMECLAW.entrySet()) {
            if (gameClawKey.startsWith(entry.getValue())) {
                return entry.getKey() + gameClawKey.substring(entry.getValue().length());
            }
        }
        return gameClawKey;
    }
}
