package ai.gameclaw.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "gameclaw.security.rbac.enabled", havingValue = "true", matchIfMissing = true)
public class DefaultRbacService implements RbacService {

    private static final Logger log = LoggerFactory.getLogger(DefaultRbacService.class);

    private final JdbcTemplate jdbc;
    private final Cache<UserProjectKey, Set<Role>> roleCache;
    private final Cache<ToolRoleKey, RiskLevel> permissionCache;
    private final Map<Role, Set<RiskLevel>> fallbackMatrix;

    public DefaultRbacService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        this.roleCache = Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(Duration.ofSeconds(30))
                .build();
        this.permissionCache = Caffeine.newBuilder()
                .maximumSize(5000)
                .expireAfterWrite(Duration.ofSeconds(30))
                .build();
        this.fallbackMatrix = buildFallbackMatrix();
    }

    @Override
    public boolean canInvoke(TenantContext ctx, String tool, RiskLevel risk) {
        if (ctx.roles() == null || ctx.roles().isEmpty()) {
            return false;
        }
        for (Role role : ctx.roles()) {
            RiskLevel maxRisk = getMaxRisk(tool, role);
            if (maxRisk != null && risk.ordinal() <= maxRisk.ordinal()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void assertCanInvoke(TenantContext ctx, String tool, RiskLevel risk) {
        if (!canInvoke(ctx, tool, risk)) {
            log.warn("[RbacService] Access denied: userId={}, tool={}, riskLevel={}, roles={}",
                    ctx.userId(), tool, risk, ctx.roles());
            throw new SecurityException(
                    String.format("Access denied: roles %s cannot invoke tool '%s' at risk level %s",
                            ctx.roles(), tool, risk));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Role> getUserRoles(UUID userId, UUID projectId) {
        UserProjectKey key = new UserProjectKey(userId, projectId);
        Set<Role> cached = roleCache.getIfPresent(key);
        if (cached != null) return cached;

        try {
            List<String> roleCodes = jdbc.queryForList(
                    "SELECT role FROM user_roles WHERE user_id = ? AND (project_id = ? OR project_id IS NULL)",
                    String.class, userId, projectId);
            Set<Role> roles = new HashSet<>();
            for (String code : roleCodes) {
                try {
                    roles.add(Role.valueOf(code));
                } catch (IllegalArgumentException e) {
                    log.warn("[RbacService] Unknown role code: {}", code);
                }
            }
            roleCache.put(key, roles);
            return roles;
        } catch (Exception e) {
            log.warn("[RbacService] Failed to query user roles from DB, using fallback: {}", e.getMessage());
            return Set.of();
        }
    }

    @Override
    @Transactional
    public void grantRole(UUID userId, Role role, UUID projectId, UUID grantedBy) {
        jdbc.update(
                "INSERT INTO user_roles (user_id, role, project_id, granted_by) VALUES (?, ?, ?, ?) " +
                        "ON CONFLICT (user_id, role, project_id) DO NOTHING",
                userId, role.name(), projectId, grantedBy);
        roleCache.invalidate(new UserProjectKey(userId, projectId));
        log.info("[RbacService] Granted role {} to user {} in project {}", role, userId, projectId);
    }

    @Override
    @Transactional
    public void revokeRole(UUID userId, Role role, UUID projectId) {
        jdbc.update(
                "DELETE FROM user_roles WHERE user_id = ? AND role = ? AND project_id = ?",
                userId, role.name(), projectId);
        roleCache.invalidate(new UserProjectKey(userId, projectId));
        log.info("[RbacService] Revoked role {} from user {} in project {}", role, userId, projectId);
    }

    private RiskLevel getMaxRisk(String tool, Role role) {
        ToolRoleKey key = new ToolRoleKey(tool, role);
        RiskLevel cached = permissionCache.getIfPresent(key);
        if (cached != null) return cached;

        try {
            String maxRiskStr = jdbc.queryForObject(
                    "SELECT max_risk FROM tool_permissions WHERE tool = ? AND role = ?",
                    String.class, tool, role.name());
            if (maxRiskStr != null) {
                RiskLevel maxRisk = RiskLevel.valueOf(maxRiskStr);
                permissionCache.put(key, maxRisk);
                return maxRisk;
            }
        } catch (EmptyResultDataAccessException e) {
            // fall through to fallback
        } catch (Exception e) {
            log.debug("[RbacService] DB query failed for tool_permissions, using fallback: {}", e.getMessage());
        }

        Set<RiskLevel> allowed = fallbackMatrix.getOrDefault(role, Set.of());
        if (allowed.isEmpty()) return null;
        RiskLevel max = null;
        for (RiskLevel rl : allowed) {
            if (max == null || rl.ordinal() > max.ordinal()) {
                max = rl;
            }
        }
        return max;
    }

    private Map<Role, Set<RiskLevel>> buildFallbackMatrix() {
        Map<Role, Set<RiskLevel>> matrix = new EnumMap<>(Role.class);
        matrix.put(Role.PLATFORM_ADMIN, Set.of(RiskLevel.L1_READ, RiskLevel.L2_SANDBOX_WRITE, RiskLevel.L3_PROJECT_WRITE, RiskLevel.L4_DB_WRITE, RiskLevel.L5_PRODUCTION));
        matrix.put(Role.ADMIN, Set.of(RiskLevel.L1_READ, RiskLevel.L2_SANDBOX_WRITE, RiskLevel.L3_PROJECT_WRITE, RiskLevel.L4_DB_WRITE));
        matrix.put(Role.PROGRAMMER, Set.of(RiskLevel.L1_READ, RiskLevel.L2_SANDBOX_WRITE, RiskLevel.L3_PROJECT_WRITE));
        matrix.put(Role.QA, Set.of(RiskLevel.L1_READ, RiskLevel.L2_SANDBOX_WRITE));
        matrix.put(Role.DATA_ANALYST, Set.of(RiskLevel.L1_READ, RiskLevel.L2_SANDBOX_WRITE));
        matrix.put(Role.DEVOPS, Set.of(RiskLevel.L1_READ, RiskLevel.L2_SANDBOX_WRITE, RiskLevel.L3_PROJECT_WRITE, RiskLevel.L4_DB_WRITE));
        matrix.put(Role.PLANNER, Set.of(RiskLevel.L1_READ, RiskLevel.L2_SANDBOX_WRITE));
        matrix.put(Role.OPERATIONS, Set.of(RiskLevel.L1_READ, RiskLevel.L2_SANDBOX_WRITE));
        matrix.put(Role.TA, Set.of(RiskLevel.L1_READ, RiskLevel.L2_SANDBOX_WRITE, RiskLevel.L3_PROJECT_WRITE));
        matrix.put(Role.PROJECT_MANAGER, Set.of(RiskLevel.L1_READ, RiskLevel.L2_SANDBOX_WRITE, RiskLevel.L3_PROJECT_WRITE));
        return matrix;
    }

    record UserProjectKey(UUID userId, UUID projectId) {}
    record ToolRoleKey(String tool, Role role) {}
}
