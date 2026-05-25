package ai.gameclaw.onboarding.steps;

import ai.gameclaw.configuration.ConfigurationManager;
import ai.gameclaw.onboarding.OnboardingProvider;
import ai.gameclaw.security.DefaultRbacService;
import ai.gameclaw.security.Role;
import ai.gameclaw.security.RbacService;
import ai.gameclaw.security.TenantContext;
import ai.gameclaw.security.TenantContextHolder;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@Order(55)
public class S10_RoleBindingStep implements OnboardingProvider {

    private final RbacService rbacService;

    public S10_RoleBindingStep(RbacService rbacService) {
        this.rbacService = rbacService;
    }

    @Override
    public String getStepId() {
        return "role-binding";
    }

    @Override
    public String getStepTitle() {
        return "Role Binding";
    }

    @Override
    public String getTemplatePath() {
        return "onboarding/steps/S10-role-binding";
    }

    @Override
    public void prepareModel(Map<String, Object> session, Map<String, Object> model) {
        List<RoleInfo> roles = Arrays.stream(Role.values())
                .map(r -> new RoleInfo(r.name(), r.getDescription()))
                .toList();
        model.put("roles", roles);
        model.put("selectedRole", session.getOrDefault("selectedRole", "PROGRAMMER"));
    }

    @Override
    public String processStep(Map<String, String> formParams, Map<String, Object> session) {
        String selectedRole = formParams.get("role");
        if (selectedRole == null || selectedRole.isBlank()) {
            return "Please select a role";
        }
        try {
            Role.valueOf(selectedRole);
        } catch (IllegalArgumentException e) {
            return "Invalid role: " + selectedRole;
        }
        session.put("selectedRole", selectedRole);
        return null;
    }

    @Override
    public void saveConfiguration(Map<String, Object> session, ConfigurationManager configurationManager) throws Exception {
        String selectedRole = (String) session.get("selectedRole");
        if (selectedRole == null) return;

        TenantContext ctx = TenantContextHolder.tryGet().orElse(null);
        if (ctx == null || ctx.userId() == null) return;

        Role role = Role.valueOf(selectedRole);
        rbacService.grantRole(ctx.userId(), role, ctx.projectId(), ctx.userId());
    }

    @Override
    public boolean isOptional() {
        return true;
    }

    public record RoleInfo(String code, String description) {}
}
