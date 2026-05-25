package ai.gameclaw.security;

import java.util.Set;
import java.util.UUID;

public interface RbacService {

    boolean canInvoke(TenantContext ctx, String tool, RiskLevel risk);

    void assertCanInvoke(TenantContext ctx, String tool, RiskLevel risk);

    Set<Role> getUserRoles(UUID userId, UUID projectId);

    void grantRole(UUID userId, Role role, UUID projectId, UUID grantedBy);

    void revokeRole(UUID userId, Role role, UUID projectId);
}
