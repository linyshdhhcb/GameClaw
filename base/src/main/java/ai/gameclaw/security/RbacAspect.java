package ai.gameclaw.security;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Aspect
@Component
@ConditionalOnProperty(name = "gameclaw.security.rbac.enabled", havingValue = "true", matchIfMissing = true)
public class RbacAspect {

    private final RbacService rbacService;

    public RbacAspect(RbacService rbacService) {
        this.rbacService = rbacService;
    }

    @Around("@annotation(ai.gameclaw.security.RequireRole)")
    public Object enforceRole(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        RequireRole annotation = sig.getMethod().getAnnotation(RequireRole.class);
        TenantContext ctx = TenantContextHolder.require();

        Set<Role> requiredRoles = Arrays.stream(annotation.value()).collect(Collectors.toSet());
        boolean hasRole = ctx.roles().stream()
                .anyMatch(tenantRole -> requiredRoles.contains(rbacService.mapRole(tenantRole)));

        if (!hasRole) {
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
        rbacService.assertCanInvoke(ctx, toolName, annotation.value());

        return pjp.proceed();
    }
}
