package ai.gameclaw.persistence;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
@ConditionalOnProperty(name = "gameclaw.multi-tenancy.enabled", havingValue = "true")
public class TenantAwareDataSourceConfig {

    @Bean
    public DataSource dataSource(org.springframework.core.env.Environment env) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(env.getProperty("spring.datasource.url", "jdbc:postgresql://localhost:5432/gameclaw"));
        config.setUsername(env.getProperty("spring.datasource.username", "gameclaw_app"));
        config.setPassword(env.getProperty("spring.datasource.password", "gameclaw_app_pwd"));

        String defaultTenantId = env.getProperty("gameclaw.multi-tenancy.default-tenant-id",
                "00000000-0000-0000-0000-000000000000");
        config.setConnectionInitSql("RESET ALL; SET gameclaw.tenant_id = '" + defaultTenantId + "'");

        config.setPoolName("gameclaw-tenant-pool");
        config.setMaximumPoolSize(20);
        config.setMinimumIdle(5);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        return new HikariDataSource(config);
    }
}
