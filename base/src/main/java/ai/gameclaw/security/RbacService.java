package ai.gameclaw.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

@Component
public class RbacService {

    private static final Logger log = LoggerFactory.getLogger(RbacService.class);

    private final Map<Role, Set<RiskLevel>> roleRiskMatrix = new EnumMap<>(Role.class);

    public RbacService() {
        roleRiskMatrix.put(Role.PLATFORM_ADMIN, Set.of(RiskLevel.L1_READ, RiskLevel.L2_SANDBOX_WRITE, RiskLevel.L3_PROJECT_WRITE, RiskLevel.L4_DB_WRITE, RiskLevel.L5_PRODUCTION));
        roleRiskMatrix.put(Role.ADMIN, Set.of(RiskLevel.L1_READ, RiskLevel.L2_SANDBOX_WRITE, RiskLevel.L3_PROJECT_WRITE, RiskLevel.L4_DB_WRITE));
        roleRiskMatrix.put(Role.PROGRAMMER, Set.of(RiskLevel.L1_READ, RiskLevel.L2_SANDBOX_WRITE, RiskLevel.L3_PROJECT_WRITE));
        roleRiskMatrix.put(Role.QA, Set.of(RiskLevel.L1_READ, RiskLevel.L2_SANDBOX_WRITE));
        roleRiskMatrix.put(Role.DATA_ANALYST, Set.of(RiskLevel.L1_READ, RiskLevel.L2_SANDBOX_WRITE));
        roleRiskMatrix.put(Role.DEVOPS, Set.of(RiskLevel.L1_READ, RiskLevel.L2_SANDBOX_WRITE, RiskLevel.L3_PROJECT_WRITE, RiskLevel.L4_DB_WRITE));
        roleRiskMatrix.put(Role.PLANNER, Set.of(RiskLevel.L1_READ, RiskLevel.L2_SANDBOX_WRITE));
        roleRiskMatrix.put(Role.OPERATIONS, Set.of(RiskLevel.L1_READ, RiskLevel.L2_SANDBOX_WRITE));
        roleRiskMatrix.put(Role.TA, Set.of(RiskLevel.L1_READ));
        roleRiskMatrix.put(Role.PROJECT_MANAGER, Set.of(RiskLevel.L1_READ, RiskLevel.L2_SANDBOX_WRITE, RiskLevel.L3_PROJECT_WRITE));
    }

    public boolean canInvoke(TenantContext ctx, String tool, RiskLevel risk) {
        if (ctx.roles() == null || ctx.roles().isEmpty()) {
            return false;
        }
        for (TenantContext.Role tenantRole : ctx.roles()) {
            Role mapped = mapRole(tenantRole);
            Set<RiskLevel> allowed = roleRiskMatrix.getOrDefault(mapped, Set.of());
            if (allowed.contains(risk)) {
                return true;
            }
        }
        return false;
    }

    public void assertCanInvoke(TenantContext ctx, String tool, RiskLevel risk) {
        if (!canInvoke(ctx, tool, risk)) {
            log.warn("[RbacService] Access denied: userId={}, tool={}, riskLevel={}", ctx.userId(), tool, risk);
            throw new SecurityException(
                    String.format("Access denied: role cannot invoke tool '%s' at risk level %s", tool, risk));
        }
    }

    public Role mapRole(TenantContext.Role tenantRole) {
        return switch (tenantRole) {
            case ADMIN -> Role.ADMIN;
            case MEMBER -> Role.PROGRAMMER;
            case VIEWER -> Role.TA;
        };
    }
}
