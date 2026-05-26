package ai.gameclaw.tools.data;

import ai.gameclaw.agent.llm.ChatRequest;
import ai.gameclaw.agent.llm.ChatResponse;
import ai.gameclaw.agent.llm.LlmClient;
import ai.gameclaw.governance.ValidationGate;
import ai.gameclaw.observability.AiMetrics;
import ai.gameclaw.security.Role;
import ai.gameclaw.security.TenantContext;
import ai.gameclaw.security.TenantContextHolder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameDataToolTest {

    private static final UUID T1 = UUID.randomUUID();
    private static final UUID P1 = UUID.randomUUID();
    private static final UUID U1 = UUID.randomUUID();

    @Mock
    LlmClient llmClient;

    @Mock(lenient = true)
    ObjectProvider<AiMetrics> aiMetricsProvider;

    List<ValidationGate> gates = List.of();

    private String runWithTenant(GameDataTool tool, String question, String table) {
        try {
            return TenantContextHolder.runWith(
                    TenantContext.of(T1, P1, U1, Set.of(Role.DATA_ANALYST)),
                    () -> tool.queryData(question, table)
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private GameDataTool createTool() {
        when(aiMetricsProvider.getIfAvailable()).thenReturn(null);
        return new GameDataTool(llmClient, new SqlSafetyValidator(), gates, aiMetricsProvider);
    }

    @Test
    void queryDataGeneratesSqlAndReturnsResult() {
        GameDataTool tool = createTool();
        when(llmClient.call(any(ChatRequest.class))).thenReturn(
                new ChatResponse("SELECT count() AS dau FROM events WHERE event_date >= today() - 7")
        );

        String result = runWithTenant(tool, "近7天DAU趋势", "events");

        assertThat(result).contains("SQL:");
        assertThat(result).contains("SELECT");
    }

    @Test
    void queryDataBlocksDangerousSql() {
        GameDataTool tool = createTool();
        when(llmClient.call(any(ChatRequest.class))).thenReturn(
                new ChatResponse("DELETE FROM events WHERE 1=1")
        );

        String result = runWithTenant(tool, "删除所有事件", "events");

        assertThat(result).contains("BLOCKED");
    }

    @Test
    void queryDataStripsMarkdownFence() {
        GameDataTool tool = createTool();
        when(llmClient.call(any(ChatRequest.class))).thenReturn(
                new ChatResponse("```sql\nSELECT * FROM events LIMIT 10\n```")
        );

        String result = runWithTenant(tool, "查看事件表", "events");

        assertThat(result).contains("SELECT * FROM events");
        assertThat(result).doesNotContain("```");
    }

    @Test
    void queryDataReturnsErrorWithoutTenantContext() {
        GameDataTool tool = createTool();

        String result = tool.queryData("近7天DAU", null);

        assertThat(result).contains("缺少租户上下文");
    }
}
