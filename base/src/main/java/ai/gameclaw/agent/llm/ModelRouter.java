package ai.gameclaw.agent.llm;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.regex.Pattern;

@Component
public class ModelRouter {

    private static final Set<String> COMPLEX_KEYWORDS = Set.of(
            "分析", "推理", "设计", "architect", "design", "analyze"
    );

    private static final int LONG_PROMPT_THRESHOLD = 4000;
    private static final int SHORT_PROMPT_THRESHOLD = 200;

    private static final Pattern COMPLEX_PATTERN = Pattern.compile(
            "分析|推理|设计|architect|design|analyze",
            Pattern.CASE_INSENSITIVE
    );

    private final ModelRouterProperties properties;

    public ModelRouter(ModelRouterProperties properties) {
        this.properties = properties;
    }

    public ModelChoice route(ChatRequest request) {
        Complexity complexity = classify(request.prompt());
        String modelId = properties.getModelMap().getOrDefault(complexity, "sonnet");
        String fallback = properties.getFallbackMap().getOrDefault(complexity, "sonnet");
        return new ModelChoice(modelId, fallback, complexity);
    }

    Complexity classify(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return Complexity.STANDARD;
        }

        boolean hasComplexKeywords = COMPLEX_PATTERN.matcher(prompt).find();

        if (hasComplexKeywords) {
            return Complexity.COMPLEX;
        }

        if (prompt.length() > LONG_PROMPT_THRESHOLD) {
            return Complexity.STANDARD;
        }

        if (prompt.length() < SHORT_PROMPT_THRESHOLD) {
            return Complexity.SIMPLE;
        }

        return Complexity.STANDARD;
    }
}
