package ai.gameclaw.chat.api;

import ai.gameclaw.security.RbacService;
import ai.gameclaw.security.Role;
import ai.gameclaw.security.TenantContext;
import ai.gameclaw.security.TenantContextHolder;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleApiControllerTest {

    @Mock
    private RbacService rbacService;

    @Mock
    private JdbcTemplate jdbc;

    private RoleApiController controller;

    @BeforeEach
    void setUp() {
        controller = new RoleApiController(rbacService, jdbc);
    }

    @Test
    void listRolesWithoutTenant() {
        List<Map<String, String>> roles = controller.listRoles();

        assertThat(roles).hasSize(Role.values().length);
        assertThat(roles.get(0)).containsKey("name");
        assertThat(roles.get(0)).containsKey("description");
    }

    @Test
    void listRolesWithTenant() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        TenantContext ctx = TenantContext.of(tenantId, projectId, userId, Set.of(Role.PROGRAMMER));

        when(rbacService.getUserRoles(userId, projectId)).thenReturn(Set.of(Role.PROGRAMMER));

        List<Map<String, String>> roles = TenantContextHolder.runWith(ctx,
                (ScopedValue.CallableOp<List<Map<String, String>>, Exception>) controller::listRoles);

        assertThat(roles).hasSize(Role.values().length);
        Map<String, String> programmerRole = roles.stream()
                .filter(r -> "PROGRAMMER".equals(r.get("name")))
                .findFirst()
                .orElseThrow();
        assertThat(programmerRole.get("active")).isEqualTo("true");
    }

    @Test
    void listRolesMarksInactiveRoles() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        TenantContext ctx = TenantContext.of(tenantId, projectId, userId, Set.of(Role.PROGRAMMER));

        when(rbacService.getUserRoles(userId, projectId)).thenReturn(Set.of(Role.PROGRAMMER));

        List<Map<String, String>> roles = TenantContextHolder.runWith(ctx,
                (ScopedValue.CallableOp<List<Map<String, String>>, Exception>) controller::listRoles);

        Map<String, String> adminRole = roles.stream()
                .filter(r -> "ADMIN".equals(r.get("name")))
                .findFirst()
                .orElseThrow();
        assertThat(adminRole.get("active")).isEqualTo("false");
    }

    @Test
    void toolsForRoleReturnsFallbackWhenDbEmpty() {
        when(jdbc.queryForList(anyString(), anyString())).thenReturn(List.of());

        Map<String, Object> result = controller.toolsForRole("PROGRAMMER");

        assertThat(result.get("role")).isEqualTo("PROGRAMMER");
        assertThat(result.get("description")).isEqualTo(Role.PROGRAMMER.getDescription());
        assertThat(result.get("source")).isEqualTo("fallback");
        assertThat(result.get("tools")).isInstanceOf(List.class);
    }

    @Test
    void toolsForRoleReturnsDatabaseWhenDbHasData() {
        Map<String, Object> toolEntry = Map.of(
                "tool", "game_code_tool",
                "max_risk", "L3_PROJECT_WRITE",
                "requires_approval", false
        );
        when(jdbc.queryForList(anyString(), anyString())).thenReturn(List.of(toolEntry));

        Map<String, Object> result = controller.toolsForRole("PROGRAMMER");

        assertThat(result.get("role")).isEqualTo("PROGRAMMER");
        assertThat(result.get("source")).isEqualTo("database");
        assertThat(result.get("tools")).isInstanceOf(List.class);
    }

    @Test
    void toolsForInvalidRole() {
        Map<String, Object> result = controller.toolsForRole("INVALID");

        assertThat(result).containsKey("error");
        assertThat(result.get("error").toString()).contains("INVALID");
    }

    @Test
    void switchRoleSetsSessionAttribute() {
        HttpSession session = mock(HttpSession.class);

        Map<String, Object> result = controller.switchRole("PROGRAMMER", session);

        assertThat(result.get("status")).isEqualTo("ok");
        assertThat(result.get("role")).isEqualTo("PROGRAMMER");
        assertThat(result.get("description")).isEqualTo(Role.PROGRAMMER.getDescription());
        verify(session).setAttribute("currentRole", "PROGRAMMER");
    }

    @Test
    void switchRoleWithInvalidRoleReturnsError() {
        HttpSession session = mock(HttpSession.class);

        Map<String, Object> result = controller.switchRole("INVALID", session);

        assertThat(result).containsKey("error");
        assertThat(result.get("error").toString()).contains("INVALID");
    }
}
