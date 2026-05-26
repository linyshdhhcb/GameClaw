package ai.gameclaw.cost;

import ai.gameclaw.observability.AiMetrics;
import ai.gameclaw.security.TenantContext;
import ai.gameclaw.security.TenantContextHolder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@EnableConfigurationProperties(QuotaProperties.class)
public class JdbcQuotaManager implements QuotaManager {

    private final JdbcTemplate jdbc;
    private final ObjectProvider<AiMetrics> aiMetrics;
    private final QuotaProperties properties;

    public JdbcQuotaManager(JdbcTemplate jdbc, ObjectProvider<AiMetrics> aiMetrics, QuotaProperties properties) {
        this.jdbc = jdbc;
        this.aiMetrics = aiMetrics;
        this.properties = properties;
    }

    @Override
    public boolean checkQuota(String tenantId, String resource) {
        if (!properties.enabled()) return true;

        UUID tid = UUID.fromString(tenantId);
        TenantContext ctx = TenantContextHolder.tryGet().orElse(null);
        UUID userId = ctx != null ? ctx.userId() : null;
        UUID projectId = ctx != null ? ctx.projectId() : null;

        ensureQuotasExist(tid, projectId, userId, resource);
        return findExceededQuotas(tid, projectId, userId, resource).isEmpty();
    }

    @Override
    @Transactional
    public void consumeQuota(String tenantId, String resource, double amount) {
        if (!properties.enabled()) return;

        UUID tid = UUID.fromString(tenantId);
        TenantContext ctx = TenantContextHolder.tryGet().orElse(null);
        UUID userId = ctx != null ? ctx.userId() : null;
        UUID projectId = ctx != null ? ctx.projectId() : null;

        ensureQuotasExist(tid, projectId, userId, resource);
        incrementUsage(tid, projectId, userId, resource, amount);

        List<String> exceeded = findExceededQuotas(tid, projectId, userId, resource);
        if (!exceeded.isEmpty()) {
            AiMetrics metrics = aiMetrics.getIfAvailable();
            if (metrics != null) {
                metrics.recordQuotaExhausted(projectId != null ? projectId.toString() : tenantId);
            }
            throw new QuotaExhaustedException(QuotaType.valueOf(exceeded.get(0)));
        }
    }

    @Override
    public double getRemainingQuota(String tenantId, String resource) {
        if (!properties.enabled()) return Double.MAX_VALUE;

        UUID tid = UUID.fromString(tenantId);
        TenantContext ctx = TenantContextHolder.tryGet().orElse(null);
        UUID userId = ctx != null ? ctx.userId() : null;
        UUID projectId = ctx != null ? ctx.projectId() : null;

        ensureQuotasExist(tid, projectId, userId, resource);

        double minRemaining = Double.MAX_VALUE;

        Double userRemaining = queryRemaining(tid, userId, null, resource, QuotaType.USER_DAILY);
        if (userRemaining != null) minRemaining = Math.min(minRemaining, userRemaining);

        Double projectRemaining = queryRemaining(tid, null, projectId, resource, QuotaType.PROJECT_MONTHLY);
        if (projectRemaining != null) minRemaining = Math.min(minRemaining, projectRemaining);

        Double globalRemaining = queryRemaining(tid, null, null, resource, QuotaType.GLOBAL_DAILY);
        if (globalRemaining != null) minRemaining = Math.min(minRemaining, globalRemaining);

        return minRemaining == Double.MAX_VALUE ? 0.0 : minRemaining;
    }

    private void ensureQuotasExist(UUID tenantId, UUID projectId, UUID userId, String resource) {
        if (userId != null) {
            ensureQuotaExists(tenantId, projectId, userId, resource, QuotaType.USER_DAILY, properties.userDailyLimit());
        }
        if (projectId != null) {
            ensureQuotaExists(tenantId, projectId, null, resource, QuotaType.PROJECT_MONTHLY, properties.projectMonthlyLimit());
        }
        ensureQuotaExists(tenantId, null, null, resource, QuotaType.GLOBAL_DAILY, properties.globalDailyLimit());
    }

    private void ensureQuotaExists(UUID tenantId, UUID projectId, UUID userId, String resource, QuotaType type, double limit) {
        Integer count = countActiveQuota(tenantId, projectId, userId, type, resource);
        if (count == null || count == 0) {
            createQuota(tenantId, projectId, userId, resource, type, limit);
        }
    }

    private Integer countActiveQuota(UUID tenantId, UUID projectId, UUID userId, QuotaType type, String resource) {
        if (userId != null && projectId != null) {
            return jdbc.queryForObject(
                    "SELECT COUNT(*) FROM quotas WHERE tenant_id = ? AND project_id = ? AND user_id = ? AND quota_type = ? AND resource = ? AND period_start <= NOW() AND period_end > NOW()",
                    Integer.class, tenantId, projectId, userId, type.name(), resource);
        } else if (projectId != null) {
            return jdbc.queryForObject(
                    "SELECT COUNT(*) FROM quotas WHERE tenant_id = ? AND project_id = ? AND user_id IS NULL AND quota_type = ? AND resource = ? AND period_start <= NOW() AND period_end > NOW()",
                    Integer.class, tenantId, projectId, type.name(), resource);
        } else if (userId != null) {
            return jdbc.queryForObject(
                    "SELECT COUNT(*) FROM quotas WHERE tenant_id = ? AND project_id IS NULL AND user_id = ? AND quota_type = ? AND resource = ? AND period_start <= NOW() AND period_end > NOW()",
                    Integer.class, tenantId, userId, type.name(), resource);
        } else {
            return jdbc.queryForObject(
                    "SELECT COUNT(*) FROM quotas WHERE tenant_id = ? AND project_id IS NULL AND user_id IS NULL AND quota_type = ? AND resource = ? AND period_start <= NOW() AND period_end > NOW()",
                    Integer.class, tenantId, type.name(), resource);
        }
    }

    private void createQuota(UUID tenantId, UUID projectId, UUID userId, String resource, QuotaType type, double limit) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime periodStart;
        LocalDateTime periodEnd;

        switch (type) {
            case USER_DAILY, GLOBAL_DAILY -> {
                periodStart = now.toLocalDate().atStartOfDay();
                periodEnd = periodStart.plusDays(1);
            }
            case PROJECT_MONTHLY -> {
                periodStart = now.toLocalDate().withDayOfMonth(1).atStartOfDay();
                periodEnd = periodStart.plusMonths(1);
            }
            default -> throw new IllegalArgumentException("Unknown quota type: " + type);
        }

        jdbc.update(
                "INSERT INTO quotas (tenant_id, project_id, user_id, quota_type, resource, limit_amount, used_amount, period_start, period_end) " +
                        "VALUES (?, ?, ?, ?, ?, ?, 0, ?, ?)",
                tenantId, projectId, userId, type.name(), resource, limit, periodStart, periodEnd);
    }

    private void incrementUsage(UUID tenantId, UUID projectId, UUID userId, String resource, double amount) {
        if (userId != null) {
            jdbc.update(
                    "UPDATE quotas SET used_amount = used_amount + ?, updated_at = NOW() " +
                            "WHERE tenant_id = ? AND user_id = ? AND quota_type = ? AND resource = ? " +
                            "AND period_start <= NOW() AND period_end > NOW()",
                    amount, tenantId, userId, QuotaType.USER_DAILY.name(), resource);
        }

        if (projectId != null) {
            jdbc.update(
                    "UPDATE quotas SET used_amount = used_amount + ?, updated_at = NOW() " +
                            "WHERE tenant_id = ? AND project_id = ? AND quota_type = ? AND resource = ? " +
                            "AND period_start <= NOW() AND period_end > NOW()",
                    amount, tenantId, projectId, QuotaType.PROJECT_MONTHLY.name(), resource);
        }

        jdbc.update(
                "UPDATE quotas SET used_amount = used_amount + ?, updated_at = NOW() " +
                        "WHERE tenant_id = ? AND quota_type = ? AND resource = ? " +
                        "AND project_id IS NULL AND user_id IS NULL " +
                        "AND period_start <= NOW() AND period_end > NOW()",
                amount, tenantId, QuotaType.GLOBAL_DAILY.name(), resource);
    }

    private List<String> findExceededQuotas(UUID tenantId, UUID projectId, UUID userId, String resource) {
        StringBuilder sql = new StringBuilder(
                "SELECT quota_type FROM quotas WHERE tenant_id = ? AND resource = ? " +
                        "AND used_amount > limit_amount AND period_start <= NOW() AND period_end > NOW() AND (");
        List<Object> params = new ArrayList<>();
        params.add(tenantId);
        params.add(resource);

        boolean first = true;

        if (userId != null) {
            sql.append("(quota_type = ? AND user_id = ?)");
            params.add(QuotaType.USER_DAILY.name());
            params.add(userId);
            first = false;
        }

        if (projectId != null) {
            if (!first) sql.append(" OR ");
            sql.append("(quota_type = ? AND project_id = ?)");
            params.add(QuotaType.PROJECT_MONTHLY.name());
            params.add(projectId);
            first = false;
        }

        if (!first) sql.append(" OR ");
        sql.append("(quota_type = ? AND project_id IS NULL AND user_id IS NULL)");
        params.add(QuotaType.GLOBAL_DAILY.name());

        sql.append(")");

        return jdbc.queryForList(sql.toString(), String.class, params.toArray());
    }

    private Double queryRemaining(UUID tenantId, UUID userId, UUID projectId, String resource, QuotaType type) {
        String sql;
        Object[] params;

        if (type == QuotaType.USER_DAILY && userId != null) {
            sql = "SELECT limit_amount - used_amount FROM quotas " +
                    "WHERE tenant_id = ? AND user_id = ? AND quota_type = ? AND resource = ? " +
                    "AND period_start <= NOW() AND period_end > NOW()";
            params = new Object[]{tenantId, userId, type.name(), resource};
        } else if (type == QuotaType.PROJECT_MONTHLY && projectId != null) {
            sql = "SELECT limit_amount - used_amount FROM quotas " +
                    "WHERE tenant_id = ? AND project_id = ? AND quota_type = ? AND resource = ? " +
                    "AND period_start <= NOW() AND period_end > NOW()";
            params = new Object[]{tenantId, projectId, type.name(), resource};
        } else if (type == QuotaType.GLOBAL_DAILY) {
            sql = "SELECT limit_amount - used_amount FROM quotas " +
                    "WHERE tenant_id = ? AND quota_type = ? AND resource = ? " +
                    "AND project_id IS NULL AND user_id IS NULL " +
                    "AND period_start <= NOW() AND period_end > NOW()";
            params = new Object[]{tenantId, type.name(), resource};
        } else {
            return null;
        }

        try {
            return jdbc.queryForObject(sql, Double.class, params);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }
}
