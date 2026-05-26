package ai.gameclaw.tools.data;

import ai.gameclaw.agent.llm.ChatRequest;
import ai.gameclaw.agent.llm.ChatResponse;
import ai.gameclaw.agent.llm.LlmClient;
import ai.gameclaw.governance.ValidatedLlmOutput;
import ai.gameclaw.governance.ValidationGate;
import ai.gameclaw.observability.AiMetrics;
import ai.gameclaw.security.RequireRiskLevel;
import ai.gameclaw.security.RiskLevel;
import ai.gameclaw.security.TenantContext;
import ai.gameclaw.security.TenantContextHolder;
import ai.gameclaw.security.pii.PiiMaskingPostProcessor;
import ai.gameclaw.tools.GameTool;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
@GameTool
public class GameDataTool {

    private static final Logger log = LoggerFactory.getLogger(GameDataTool.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String NL_TO_SQL_PROMPT = """
            You are a ClickHouse SQL expert. Generate a single SELECT query for the following question.
            
            Rules:
            - ONLY generate SELECT statements. Never generate INSERT/UPDATE/DELETE/DDL.
            - Use proper ClickHouse SQL syntax.
            - Add LIMIT if not specified (default 1000).
            - Use table and column names from the provided schema context.
            
            Schema context:
            %s
            
            Question: %s
            
            Respond with ONLY the SQL query, no explanation.
            """;

    private final LlmClient llmClient;
    private final SqlSafetyValidator sqlValidator;
    private final List<ValidationGate> gates;
    private final ObjectProvider<AiMetrics> aiMetricsProvider;
    private final Cache<String, List<Map<String, Object>>> queryCache;

    public GameDataTool(LlmClient llmClient,
                        SqlSafetyValidator sqlValidator,
                        List<ValidationGate> gates,
                        ObjectProvider<AiMetrics> aiMetricsProvider) {
        this.llmClient = llmClient;
        this.sqlValidator = sqlValidator;
        this.gates = gates;
        this.aiMetricsProvider = aiMetricsProvider;
        this.queryCache = Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(Duration.ofMinutes(5))
                .build();
    }

    @Tool(name = "query_data", description = "用自然语言查询游戏数据仓库（仅 SELECT），返回脱敏后的结果")
    @RequireRiskLevel(RiskLevel.L1_READ)
    public String queryData(
            @ToolParam(description = "问题（如：近 7 天 DAU 趋势）") String question,
            @ToolParam(description = "首选表名（可选）", required = false) String preferredTable) {

        TenantContext ctx = TenantContextHolder.tryGet().orElse(null);
        if (ctx == null) {
            return "错误：缺少租户上下文，无法执行查询";
        }

        try {
            String schemaContext = buildSchemaContext(preferredTable);
            String sql = generateSql(question, schemaContext);
            sqlValidator.assertSelectOnly(sql);

            String cacheKey = sha256(sql + "@" + ctx.tenantId());
            List<Map<String, Object>> cached = queryCache.getIfPresent(cacheKey);
            if (cached != null) {
                log.info("[GameDataTool] Cache hit for query: {}", sql.substring(0, Math.min(sql.length(), 80)));
                return formatResult(sql, cached, true);
            }

            List<Map<String, Object>> raw = executeViaMcp(sql);
            List<Map<String, Object>> masked = PiiMaskingPostProcessor.mask(raw, ctx);
            queryCache.put(cacheKey, masked);

            return formatResult(sql, masked, false);
        } catch (SecurityException e) {
            return "BLOCKED: " + e.getMessage();
        } catch (Exception e) {
            log.error("[GameDataTool] Query failed", e);
            return "查询失败: " + e.getMessage();
        }
    }

    private String generateSql(String question, String schemaContext) {
        String prompt = String.format(NL_TO_SQL_PROMPT, schemaContext, question);
        ChatResponse response = llmClient.call(new ChatRequest(prompt, null, null));
        String sql = response.content().strip();
        if (sql.startsWith("```")) {
            sql = sql.replaceAll("^```\\w*\\n?", "").replaceAll("\\n?```$", "");
        }
        return sql.strip();
    }

    private String buildSchemaContext(String preferredTable) {
        if (preferredTable != null && !preferredTable.isBlank()) {
            return "Table: " + preferredTable + " (columns auto-discovered via MCP describe_table)";
        }
        return "Available tables auto-discovered via MCP list_tables. Use describe_table for column details.";
    }

    private List<Map<String, Object>> executeViaMcp(String sql) {
        log.info("[GameDataTool] Executing SQL via MCP: {}", sql);
        return List.of(Map.of("info", "MCP execute_query result placeholder", "sql", sql));
    }

    private String formatResult(String sql, List<Map<String, Object>> rows, boolean cacheHit) {
        StringBuilder sb = new StringBuilder();
        sb.append("SQL: ").append(sql).append("\n\n");
        sb.append("结果 (").append(rows.size()).append(" 行, cache=").append(cacheHit).append("):\n\n");
        if (rows.isEmpty()) {
            sb.append("(无数据)");
            return sb.toString();
        }
        var columns = rows.getFirst().keySet().stream().toList();
        sb.append("| ").append(String.join(" | ", columns)).append(" |\n");
        sb.append("| ").append(columns.stream().map(c -> "---").reduce((a, b) -> a + " | " + b).orElse("")).append(" |\n");
        for (Map<String, Object> row : rows) {
            sb.append("| ");
            for (String col : columns) {
                sb.append(row.get(col) != null ? row.get(col) : "NULL").append(" | ");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private String sha256(String input) {
        try {
            var md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            return String.valueOf(input.hashCode());
        }
    }
}
