package ai.gameclaw.agent.llm;

import java.util.Map;

public record ChatRequest(
        String prompt,
        String conversationId,
        Map<String, Object> context
) {
    public ChatRequest(String prompt) {
        this(prompt, null, Map.of());
    }

    public ChatRequest(String prompt, String conversationId) {
        this(prompt, conversationId, Map.of());
    }
}
