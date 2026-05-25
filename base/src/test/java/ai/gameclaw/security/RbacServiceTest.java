package ai.gameclaw.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RbacServiceTest {

    private DefaultRbacService rbacService;
    private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), eq(String.class), any(Object[].class)))
                .thenThrow(new EmptyResultDataAccessException(1));
        rbacService = new DefaultRbacService(jdbc);
    }

    @Test
    void adminCanInvokeL4DbWrite() {
        TenantContext ctx = TenantContext.of(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), Set.of(Role.ADMIN));
        assertThat(rbacService.canInvoke(ctx, "db-tool", RiskLevel.L4_DB_WRITE)).isTrue();
    }

    @Test
    void taCannotInvokeL2SandboxWrite() {
        TenantContext ctx = TenantContext.of(
                UUID.randomUUID(), null, UUID.randomUUID(), Set.of(Role.TA));
        assertThat(rbacService.canInvoke(ctx, "sandbox-tool", RiskLevel.L2_SANDBOX_WRITE)).isFalse();
    }

    @Test
    void assertCanInvokeThrowsOnDenied() {
        TenantContext ctx = TenantContext.of(
                UUID.randomUUID(), null, UUID.randomUUID(), Set.of(Role.TA));
        assertThatThrownBy(() -> rbacService.assertCanInvoke(ctx, "db-tool", RiskLevel.L4_DB_WRITE))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Access denied");
    }

    @Test
    void programmerCanInvokeL3ProjectWrite() {
        TenantContext ctx = TenantContext.of(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), Set.of(Role.PROGRAMMER));
        assertThat(rbacService.canInvoke(ctx, "project-tool", RiskLevel.L3_PROJECT_WRITE)).isTrue();
    }

    @Test
    void grantRoleCallsJdbc() {
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID grantedBy = UUID.randomUUID();
        rbacService.grantRole(userId, Role.PLANNER, projectId, grantedBy);
    }

    @Test
    void revokeRoleCallsJdbc() {
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        rbacService.revokeRole(userId, Role.PLANNER, projectId);
    }
}
