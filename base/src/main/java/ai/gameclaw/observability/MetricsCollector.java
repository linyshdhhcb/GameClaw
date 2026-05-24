package ai.gameclaw.observability;

public interface MetricsCollector {

    void recordTokenUsage(String model, int promptTokens, int completionTokens);

    void recordLatency(String operation, long durationMs);

    void recordToolInvocation(String toolName, boolean success, long durationMs);
}
