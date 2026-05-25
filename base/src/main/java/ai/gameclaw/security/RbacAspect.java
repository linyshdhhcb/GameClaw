package ai.gameclaw.security;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Aspect
@Component
@ConditionalOnProperty(name = "gameclaw.security.rbac.enabled", havingValue = "true", matchIfMissing = true)
public class RbacAspect {

    private static final Logger log = LoggerFactory.getLogger(RbacAspect.class);

    private final RbacService rbacService;
    private final JdbcTemplate jdbc;

    public RbacAspect(RbacService rbacService, JdbcTemplate jdbc) {
        this.rbacService = rbacService;
        this.jdbc = jdbc;
    }

    @Around("@annotation(ai.gameclaw.security.RequireRole)")
    public Object enforceRole(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        RequireRole annotation = sig.getMethod().getAnnotation(RequireRole.class);
        TenantContext ctx = TenantContextHolder.require();

        Set<Role> requiredRoles = Arrays.stream(annotation.value()).collect(Collectors.toSet());
        boolean hasRole = ctx.roles().stream().anyMatch(requiredRoles::contains);

        if (!hasRole) {
            writeAuditLog(ctx, sig.getMethod().getName(), "role_check_failed",
                    "Required " + requiredRoles + ", got " + ctx.roles());
            throw new SecurityException(
                    String.format("Role check failed: required %s for method %s", requiredRoles, sig.getMethod().getName()));
        }
        return pjp.proceed();
    }

    @Around("@annotation(ai.gameclaw.security.RequireRiskLevel)")
    public Object enforceRiskLevel(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        RequireRiskLevel annotation = sig.getMethod().getAnnotation(RequireRiskLevel.class);
        TenantContext ctx = TenantContextHolder.require();

        String toolName = sig.getMethod().getName();
        try {
            rbacService.assertCanInvoke(ctx, toolName, annotation.value());
        } catch (SecurityException e) {
            writeAuditLog(ctx, toolName, "risk_level_denied",
                    "risk=" + annotation.value() + " roles=" + ctx.roles());
            throw e;
        }

        return pjp.proceed();
    }

    private void writeAuditLog(TenantContext ctx, String toolName, String action, String detail) {
        UUID tenantId = ctx != null ? ctx.tenantId() : null;
        UUID userId = ctx != null ? ctx.userId() : null;
        try {
            jdbc.update(
                    "INSERT INTO audit_log (id, tenant_id, actor_id, action, resource, detail, created_at) " +
                            "VALUES (gen_random_uuid(), ?, ?, ?, ?, ?::jsonb, now())",
                    tenantId, userId, action, "rbac_check",
                    String.format("{\"tool\":\"%s\",\"detail\":\"%s\"}", toolName, detail));
        } catch (Exception e) {
            log.warn("[RbacAspect] Failed to write audit_log: {}", e.getMessage());
        }
    }
}
