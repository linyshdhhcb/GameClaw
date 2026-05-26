package ai.gameclaw.mcp.server.clickhouse;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

@Component
public class ClickHouseTools {

    private final JdbcTemplate jdbc;
    private final SqlSafetyValidator sqlValidator;

    public ClickHouseTools(DataSource dataSource, SqlSafetyValidator sqlValidator) {
        this.jdbc = new JdbcTemplate(dataSource);
        this.sqlValidator = sqlValidator;
    }

    @Tool(name = "list_tables", description = "列出 ClickHouse 数据仓库中所有可用的表")
    public String listTables() {
        try {
            List<Map<String, Object>> rows = jdbc.queryForList(
                    "SELECT name, engine, total_rows, formatReadableSize(total_bytes) AS size " +
                            "FROM system.tables WHERE database = currentDatabase() ORDER BY name"
            );
            if (rows.isEmpty()) return "No tables found in current database.";
            StringBuilder sb = new StringBuilder("Tables in data warehouse:\n\n");
            sb.append("| Table | Engine | Rows | Size |\n|---|---|---|---|\n");
            for (Map<String, Object> row : rows) {
                sb.append("| ").append(row.get("name")).append(" | ")
                        .append(row.get("engine")).append(" | ")
                        .append(row.get("total_rows")).append(" | ")
                        .append(row.get("size")).append(" |\n");
            }
            return sb.toString();
        } catch (Exception e) {
            return "Error listing tables: " + e.getMessage();
        }
    }

    @Tool(name = "describe_table", description = "描述 ClickHouse 表的列信息（列名、类型、注释）")
    public String describeTable(
            @ToolParam(description = "表名") String tableName) {
        try {
            List<Map<String, Object>> rows = jdbc.queryForList(
                    "SELECT name, type, comment, is_in_primary_key " +
                            "FROM system.columns WHERE table = ? AND database = currentDatabase() ORDER BY position",
                    tableName
            );
            if (rows.isEmpty()) return "Table '" + tableName + "' not found or has no columns.";
            StringBuilder sb = new StringBuilder("Columns of ").append(tableName).append(":\n\n");
            sb.append("| Column | Type | Comment | PK |\n|---|---|---|---|\n");
            for (Map<String, Object> row : rows) {
                sb.append("| ").append(row.get("name")).append(" | ")
                        .append(row.get("type")).append(" | ")
                        .append(row.get("comment") != null ? row.get("comment") : "").append(" | ")
                        .append("1".equals(row.get("is_in_primary_key")) ? "Yes" : "").append(" |\n");
            }
            return sb.toString();
        } catch (Exception e) {
            return "Error describing table: " + e.getMessage();
        }
    }

    @Tool(name = "execute_query", description = "在 ClickHouse 数据仓库执行只读 SELECT 查询")
    public String executeQuery(
            @ToolParam(description = "SQL 查询语句（仅允许 SELECT）") String sql) {
        try {
            sqlValidator.assertSelectOnly(sql);
            List<Map<String, Object>> rows = jdbc.queryForList(sql);
            if (rows.isEmpty()) return "Query returned 0 rows.";
            StringBuilder sb = new StringBuilder("Query result (").append(rows.size()).append(" rows):\n\n");
            var columns = rows.getFirst().keySet().stream().toList();
            sb.append("| ").append(String.join(" | ", columns)).append(" |\n");
            sb.append("| ").append(columns.stream().map(c -> "---").reduce((a, b) -> a + " | " + b).orElse("")).append(" |\n");
            int limit = Math.min(rows.size(), 100);
            for (int i = 0; i < limit; i++) {
                Map<String, Object> row = rows.get(i);
                sb.append("| ");
                for (String col : columns) {
                    sb.append(row.get(col) != null ? row.get(col) : "NULL").append(" | ");
                }
                sb.append("\n");
            }
            if (rows.size() > 100) {
                sb.append("\n... and ").append(rows.size() - 100).append(" more rows truncated.");
            }
            return sb.toString();
        } catch (SecurityException e) {
            return "BLOCKED: " + e.getMessage();
        } catch (Exception e) {
            return "Query error: " + e.getMessage();
        }
    }
}
