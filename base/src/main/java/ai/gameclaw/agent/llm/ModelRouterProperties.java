package ai.gameclaw.agent.llm;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.EnumMap;
import java.util.Map;

@ConfigurationProperties(prefix = "gameclaw.llm")
public class ModelRouterProperties {

    private Map<Complexity, String> modelMap = new EnumMap<>(Map.of(
            Complexity.SIMPLE, "haiku",
            Complexity.STANDARD, "sonnet",
            Complexity.COMPLEX, "opus"
    ));

    private Map<Complexity, String> fallbackMap = new EnumMap<>(Map.of(
            Complexity.SIMPLE, "sonnet",
            Complexity.STANDARD, "haiku",
            Complexity.COMPLEX, "sonnet"
    ));

    public Map<Complexity, String> getModelMap() {
        return modelMap;
    }

    public void setModelMap(Map<Complexity, String> modelMap) {
        this.modelMap = modelMap;
    }

    public Map<Complexity, String> getFallbackMap() {
        return fallbackMap;
    }

    public void setFallbackMap(Map<Complexity, String> fallbackMap) {
        this.fallbackMap = fallbackMap;
    }
}
