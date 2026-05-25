package ai.gameclaw.chat.api;

import ai.gameclaw.security.RbacService;
import ai.gameclaw.security.Role;
import ai.gameclaw.security.TenantContext;
import ai.gameclaw.security.TenantContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/roles")
public class RoleApiController {

    private final RbacService rbacService;

    public RoleApiController(RbacService rbacService) {
        this.rbacService = rbacService;
    }

    @GetMapping
    public List<Map<String, String>> listRoles() {
        TenantContext ctx = TenantContextHolder.tryGet().orElse(null);
        if (ctx == null || ctx.userId() == null) {
            return Arrays.stream(Role.values())
                    .map(r -> Map.of("name", r.name(), "description", r.getDescription()))
                    .collect(Collectors.toList());
        }
        Set<Role> userRoles = ctx.projectId() != null
                ? rbacService.getUserRoles(ctx.userId(), ctx.projectId())
                : Set.of(Role.PROGRAMMER);
        return Arrays.stream(Role.values())
                .map(r -> Map.of(
                        "name", r.name(),
                        "description", r.getDescription(),
                        "active", String.valueOf(userRoles.contains(r))
                ))
                .collect(Collectors.toList());
    }

    @GetMapping("/{role}/tools")
    public Map<String, Object> toolsForRole(@PathVariable String role) {
        try {
            Role r = Role.valueOf(role);
            return Map.of(
                    "role", r.name(),
                    "description", r.getDescription(),
                    "riskLevels", allowedRiskLevels(r)
            );
        } catch (IllegalArgumentException e) {
            return Map.of("error", "Unknown role: " + role);
        }
    }

    private List<String> allowedRiskLevels(Role role) {
        return switch (role) {
            case PLATFORM_ADMIN -> List.of("L1_READ", "L2_SANDBOX_WRITE", "L3_PROJECT_WRITE", "L4_DB_WRITE", "L5_PRODUCTION");
            case ADMIN -> List.of("L1_READ", "L2_SANDBOX_WRITE", "L3_PROJECT_WRITE", "L4_DB_WRITE");
            case PROGRAMMER, DEVOPS, PROJECT_MANAGER -> List.of("L1_READ", "L2_SANDBOX_WRITE", "L3_PROJECT_WRITE");
            case QA, DATA_ANALYST, PLANNER, OPERATIONS -> List.of("L1_READ", "L2_SANDBOX_WRITE");
            case TA -> List.of("L1_READ");
        };
    }
}
