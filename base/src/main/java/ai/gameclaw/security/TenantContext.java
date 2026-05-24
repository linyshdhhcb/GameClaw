package ai.gameclaw.security;

import java.util.Set;
import java.util.UUID;

public record TenantContext(
        UUID tenantId,
        UUID projectId,
        UUID userId,
        Set<Role> roles
) {

    public enum Role {
        ADMIN, MEMBER, VIEWER
    }

    public static TenantContext of(UUID tenantId) {
        return new TenantContext(tenantId, null, null, Set.of(Role.MEMBER));
    }

    public static TenantContext of(UUID tenantId, UUID projectId) {
        return new TenantContext(tenantId, projectId, null, Set.of(Role.MEMBER));
    }

    public static TenantContext of(UUID tenantId, UUID projectId, UUID userId, Set<Role> roles) {
        return new TenantContext(tenantId, projectId, userId, roles != null ? roles : Set.of());
    }
}
