package ai.gameclaw.mcp.server.clickhouse;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "clickhouse")
public record ClickHouseProperties(
        String url,
        String username,
        String password,
        String database
) {
    public ClickHouseProperties {
        if (url == null || url.isBlank()) url = "jdbc:clickhouse://localhost:8123/default";
        if (username == null || username.isBlank()) username = "mcp_data_warehouse";
        if (password == null) password = "";
        if (database == null || database.isBlank()) database = "default";
    }
}
