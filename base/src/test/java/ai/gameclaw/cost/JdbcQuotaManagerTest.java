package ai.gameclaw.cost;

import ai.gameclaw.observability.AiMetrics;
import ai.gameclaw.security.TenantContext;
import ai.gameclaw.security.TenantContextHolder;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = JdbcQuotaManagerTest.TestApp.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:quotatest;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "gameclaw.quota.enabled=true",
        "gameclaw.quota.user-daily-limit=1.0",
        "gameclaw.quota.project-monthly-limit=1000.0",
        "gameclaw.quota.global-daily-limit=10000.0"
})
class JdbcQuotaManagerTest {

    @SpringBootApplication(scanBasePackages = "ai.gameclaw.cost")
    static class TestApp {
        public static void main(String[] args) {
            SpringApplication.run(TestApp.class, args);
        }

        @Bean
        AiMetrics aiMetrics() {
            return new AiMetrics(new SimpleMeterRegistry());
        }
    }

    @Autowired
    private JdbcTemplate jdbc;

    private JdbcQuotaManager quotaManager;
    private UUID tenantId;
    private UUID projectId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        this.tenantId = UUID.randomUUID();
        this.projectId = UUID.randomUUID();
        this.userId = UUID.randomUUID();

        jdbc.execute("CREATE TABLE IF NOT EXISTS quotas (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "tenant_id UUID NOT NULL, " +
                "project_id UUID, " +
                "user_id UUID, " +
                "quota_type VARCHAR(20) NOT NULL, " +
                "resource VARCHAR(50) NOT NULL DEFAULT 'llm_cost_cny', " +
                "limit_amount DOUBLE NOT NULL DEFAULT 0, " +
                "used_amount DOUBLE NOT NULL DEFAULT 0, " +
                "period_start TIMESTAMP NOT NULL, " +
                "period_end TIMESTAMP NOT NULL, " +
                "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");

        ObjectProvider<AiMetrics> aiMetricsProvider = new ObjectProvider<>() {
            private final AiMetrics instance = new AiMetrics(new SimpleMeterRegistry());
            @Override public AiMetrics getObject() { return instance; }
            @Override public AiMetrics getIfAvailable() { return instance; }
            @Override public AiMetrics getIfUnique() { return instance; }
            @Override public java.util.Iterator<AiMetrics> iterator() { return java.util.List.of(instance).iterator(); }
            @Override public java.util.stream.Stream<AiMetrics> orderedStream() { return java.util.stream.Stream.of(instance); }
        };

        QuotaProperties props = new QuotaProperties(1.0, 1000.0, 10000.0, true);
        quotaManager = new JdbcQuotaManager(jdbc, aiMetricsProvider, props);
    }

    @AfterEach
    void tearDown() {
        jdbc.execute("DROP TABLE IF EXISTS quotas");
    }

    @Test
    void consumeQuotaWithinLimitSucceeds() {
        TenantContextHolder.runWith(
                TenantContext.of(tenantId, projectId, userId, Set.of()),
                () -> {
                    quotaManager.consumeQuota(tenantId.toString(), "llm_cost_cny", 0.5);
                });
    }

    @Test
    void consumeQuotaExceedingUserDailyLimitThrows() {
        TenantContextHolder.runWith(
                TenantContext.of(tenantId, projectId, userId, Set.of()),
                () -> {
                    quotaManager.consumeQuota(tenantId.toString(), "llm_cost_cny", 0.5);
                    assertThatThrownBy(() ->
                            quotaManager.consumeQuota(tenantId.toString(), "llm_cost_cny", 0.6)
                    ).isInstanceOf(QuotaExhaustedException.class)
                            .satisfies(ex -> {
                                QuotaExhaustedException qee = (QuotaExhaustedException) ex;
                                assertThat(qee.getQuotaType()).isEqualTo(QuotaType.USER_DAILY);
                            });
                });
    }

    @Test
    void consumeQuotaExceedingProjectMonthlyLimitThrows() {
        QuotaProperties props = new QuotaProperties(10000.0, 1.0, 100000.0, true);
        ObjectProvider<AiMetrics> aiMetricsProvider = new ObjectProvider<>() {
            private final AiMetrics instance = new AiMetrics(new SimpleMeterRegistry());
            @Override public AiMetrics getObject() { return instance; }
            @Override public AiMetrics getIfAvailable() { return instance; }
            @Override public AiMetrics getIfUnique() { return instance; }
            @Override public java.util.Iterator<AiMetrics> iterator() { return java.util.List.of(instance).iterator(); }
            @Override public java.util.stream.Stream<AiMetrics> orderedStream() { return java.util.stream.Stream.of(instance); }
        };
        JdbcQuotaManager strictManager = new JdbcQuotaManager(jdbc, aiMetricsProvider, props);

        TenantContextHolder.runWith(
                TenantContext.of(tenantId, projectId, userId, Set.of()),
                () -> {
                    strictManager.consumeQuota(tenantId.toString(), "llm_cost_cny", 0.5);
                    assertThatThrownBy(() ->
                            strictManager.consumeQuota(tenantId.toString(), "llm_cost_cny", 0.6)
                    ).isInstanceOf(QuotaExhaustedException.class)
                            .satisfies(ex -> {
                                QuotaExhaustedException qee = (QuotaExhaustedException) ex;
                                assertThat(qee.getQuotaType()).isEqualTo(QuotaType.PROJECT_MONTHLY);
                            });
                });
    }

    @Test
    void getRemainingQuotaReturnsCorrectRemaining() {
        TenantContextHolder.runWith(
                TenantContext.of(tenantId, projectId, userId, Set.of()),
                () -> {
                    quotaManager.consumeQuota(tenantId.toString(), "llm_cost_cny", 0.3);
                    double remaining = quotaManager.getRemainingQuota(tenantId.toString(), "llm_cost_cny");
                    assertThat(remaining).isCloseTo(0.7, org.assertj.core.data.Offset.offset(0.001));
                });
    }

    @Test
    void defaultQuotasCreatedOnFirstAccess() {
        TenantContextHolder.runWith(
                TenantContext.of(tenantId, projectId, userId, Set.of()),
                () -> {
                    boolean result = quotaManager.checkQuota(tenantId.toString(), "llm_cost_cny");
                    assertThat(result).isTrue();

                    Integer count = jdbc.queryForObject(
                            "SELECT COUNT(*) FROM quotas WHERE tenant_id = ?",
                            Integer.class, tenantId);
                    assertThat(count).isNotNull();
                    assertThat(count).isGreaterThanOrEqualTo(2);
                });
    }
}
