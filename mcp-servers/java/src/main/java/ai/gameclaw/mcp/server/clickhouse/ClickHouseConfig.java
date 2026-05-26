package ai.gameclaw.mcp.server.clickhouse;

import com.clickhouse.jdbc.ClickHouseDataSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Properties;

@Configuration
@EnableConfigurationProperties(ClickHouseProperties.class)
public class ClickHouseConfig {

    @Bean
    public DataSource clickHouseDataSource(ClickHouseProperties props) {
        try {
            Properties properties = new Properties();
            properties.setProperty("user", props.username());
            properties.setProperty("password", props.password());
            return new ClickHouseDataSource(props.url(), properties);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to create ClickHouse DataSource", e);
        }
    }
}
