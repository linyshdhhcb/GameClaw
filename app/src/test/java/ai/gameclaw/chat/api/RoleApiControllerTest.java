package ai.gameclaw.chat.api;

import ai.gameclaw.security.RbacService;
import ai.gameclaw.security.Role;
import ai.gameclaw.security.TenantContext;
import ai.gameclaw.security.TenantContextHolder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleApiControllerTest {

    @Mock
    private RbacService rbacService;

    @Test
    void listRolesWithoutTenant() {
        RoleApiController controller = new RoleApiController(rbacService);
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

        RoleApiController controller = new RoleApiController(rbacService);
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
    void toolsForRole() {
        RoleApiController controller = new RoleApiController(rbacService);
        Map<String, Object> result = controller.toolsForRole("PROGRAMMER");

        assertThat(result.get("role")).isEqualTo("PROGRAMMER");
        assertThat(result.get("riskLevels")).isInstanceOf(List.class);
    }

    @Test
    void toolsForInvalidRole() {
        RoleApiController controller = new RoleApiController(rbacService);
        Map<String, Object> result = controller.toolsForRole("INVALID");

        assertThat(result).containsKey("error");
    }
}
