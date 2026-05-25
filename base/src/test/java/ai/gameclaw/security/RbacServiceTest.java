package ai.gameclaw.security;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RbacServiceTest {

    private final RbacService rbacService = new RbacService();

    @Test
    void adminCanInvokeL4DbWrite() {
        TenantContext ctx = TenantContext.of(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), Set.of(TenantContext.Role.ADMIN));
        assertThat(rbacService.canInvoke(ctx, "db-tool", RiskLevel.L4_DB_WRITE)).isTrue();
    }

    @Test
    void viewerCannotInvokeL2SandboxWrite() {
        TenantContext ctx = TenantContext.of(
                UUID.randomUUID(), null, UUID.randomUUID(), Set.of(TenantContext.Role.VIEWER));
        assertThat(rbacService.canInvoke(ctx, "sandbox-tool", RiskLevel.L2_SANDBOX_WRITE)).isFalse();
    }

    @Test
    void assertCanInvokeThrowsOnDenied() {
        TenantContext ctx = TenantContext.of(
                UUID.randomUUID(), null, UUID.randomUUID(), Set.of(TenantContext.Role.VIEWER));
        assertThatThrownBy(() -> rbacService.assertCanInvoke(ctx, "db-tool", RiskLevel.L4_DB_WRITE))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Access denied");
    }

    @Test
    void programmerCanInvokeL3ProjectWrite() {
        TenantContext ctx = TenantContext.of(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), Set.of(TenantContext.Role.MEMBER));
        assertThat(rbacService.canInvoke(ctx, "project-tool", RiskLevel.L3_PROJECT_WRITE)).isTrue();
    }
}
