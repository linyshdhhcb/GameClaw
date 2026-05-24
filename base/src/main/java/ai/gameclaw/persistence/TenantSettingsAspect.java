package ai.gameclaw.persistence;

import ai.gameclaw.security.TenantContext;
import ai.gameclaw.security.TenantContextHolder;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Aspect
@Order(0)
@Component
@ConditionalOnProperty(name = "gameclaw.multi-tenancy.enabled", havingValue = "true")
public class TenantSettingsAspect {

    private final JdbcTemplate jdbc;

    public TenantSettingsAspect(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Around("@annotation(org.springframework.transaction.annotation.Transactional)")
    public Object setupTenantSession(ProceedingJoinPoint pjp) throws Throwable {
        TenantContext ctx = TenantContextHolder.require();
        jdbc.execute("SET LOCAL gameclaw.tenant_id = '" + sanitizeUuid(ctx.tenantId()) + "'");
        if (ctx.projectId() != null) {
            jdbc.execute("SET LOCAL gameclaw.project_id = '" + sanitizeUuid(ctx.projectId()) + "'");
        }
        return pjp.proceed();
    }

    private String sanitizeUuid(UUID u) {
        String s = u.toString();
        if (!s.matches("[0-9a-fA-F\\-]+")) {
            throw new IllegalArgumentException("Invalid UUID: " + s);
        }
        return s;
    }
}
