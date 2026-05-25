package ai.gameclaw.security;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantContextHolderTest {

    private static final UUID TENANT_A = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TENANT_B = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Test
    void requireThrowsWhenNotBound() {
        assertThatThrownBy(TenantContextHolder::require)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Tenant context not bound");
    }

    @Test
    void tryGetReturnsEmptyWhenNotBound() {
        assertThat(TenantContextHolder.tryGet()).isEmpty();
    }

    @Test
    void isBoundReturnsFalseWhenNotBound() {
        assertThat(TenantContextHolder.isBound()).isFalse();
    }

    @Test
    void runWithBindsContext() throws Exception {
        TenantContext ctx = TenantContext.of(TENANT_A);
        String result = TenantContextHolder.runWith(ctx, () -> {
            assertThat(TenantContextHolder.isBound()).isTrue();
            assertThat(TenantContextHolder.require()).isEqualTo(ctx);
            assertThat(TenantContextHolder.require().tenantId()).isEqualTo(TENANT_A);
            return "ok";
        });
        assertThat(result).isEqualTo("ok");
        assertThat(TenantContextHolder.isBound()).isFalse();
    }

    @Test
    void runWithRunnableBindsContext() {
        TenantContext ctx = TenantContext.of(TENANT_A);
        TenantContextHolder.runWith(ctx, () -> {
            assertThat(TenantContextHolder.require().tenantId()).isEqualTo(TENANT_A);
        });
        assertThat(TenantContextHolder.isBound()).isFalse();
    }

    @Test
    void scopedValueDoesNotLeakAcrossBoundaries() throws Exception {
        TenantContext ctxA = TenantContext.of(TENANT_A);
        TenantContextHolder.runWith(ctxA, () -> {
            assertThat(TenantContextHolder.require().tenantId()).isEqualTo(TENANT_A);
            TenantContext ctxB = TenantContext.of(TENANT_B);
            TenantContextHolder.runWith(ctxB, () -> {
                assertThat(TenantContextHolder.require().tenantId()).isEqualTo(TENANT_B);
            });
            assertThat(TenantContextHolder.require().tenantId()).isEqualTo(TENANT_A);
        });
        assertThat(TenantContextHolder.isBound()).isFalse();
    }

    @Test
    void tenantContextWithProjectAndUser() {
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        TenantContext ctx = TenantContext.of(TENANT_A, projectId, userId, Set.of(Role.ADMIN));
        assertThat(ctx.tenantId()).isEqualTo(TENANT_A);
        assertThat(ctx.projectId()).isEqualTo(projectId);
        assertThat(ctx.userId()).isEqualTo(userId);
        assertThat(ctx.roles()).containsExactly(Role.ADMIN);
    }

    @Test
    void singleTenantFallback() {
        SingleTenantFallback fallback = new SingleTenantFallback();
        assertThat(fallback.getDefaultTenantId())
                .isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        assertThat(fallback.defaultContext().tenantId()).isEqualTo(fallback.getDefaultTenantId());
    }
}
