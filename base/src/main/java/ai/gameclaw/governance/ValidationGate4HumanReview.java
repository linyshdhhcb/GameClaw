package ai.gameclaw.governance;

import ai.gameclaw.governance.approval.ApprovalGateway;
import ai.gameclaw.governance.approval.ApprovalNotifier;
import ai.gameclaw.governance.approval.ApprovalRequest;
import ai.gameclaw.governance.approval.PendingApproval;
import ai.gameclaw.security.RiskLevel;
import ai.gameclaw.security.TenantContext;
import ai.gameclaw.security.TenantContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@Order(4)
@ConditionalOnBean(ApprovalGateway.class)
public class ValidationGate4HumanReview implements ValidationGate {

    private static final Logger log = LoggerFactory.getLogger(ValidationGate4HumanReview.class);

    private final ApprovalGateway approvalGateway;
    private final ApprovalNotifier notifier;

    public ValidationGate4HumanReview(ApprovalGateway approvalGateway, ApprovalNotifier notifier) {
        this.approvalGateway = approvalGateway;
        this.notifier = notifier;
    }

    @Override
    public String name() {
        return "human_review";
    }

    @Override
    @SuppressWarnings("unchecked")
    public ValidationResult validate(Object output, Class<?> expectedType) {
        if (output instanceof Map<?, ?> map) {
            Object requiresApproval = map.get("requiresApproval");
            if (Boolean.TRUE.equals(requiresApproval)) {
                TenantContext ctx = TenantContextHolder.tryGet().orElse(null);
                UUID requesterId = ctx != null ? ctx.userId() : null;

                Object resourceObj = map.get("resource");
                String resource = resourceObj != null ? resourceObj.toString() : "unknown";
                Object actionObj = map.get("action");
                String action = actionObj != null ? actionObj.toString() : "unknown";
                Object riskLevelObj = map.get("riskLevel");
                String riskLevelStr = riskLevelObj != null ? riskLevelObj.toString() : "L3_PROJECT_WRITE";
                RiskLevel riskLevel = RiskLevel.valueOf(riskLevelStr);
                Object impactSummaryObj = map.get("impactSummary");
                String impactSummary = impactSummaryObj != null ? impactSummaryObj.toString() : "";
                int quorum = map.containsKey("quorum") ? ((Number) map.get("quorum")).intValue() : 1;
                long ttlMinutes = map.containsKey("ttlMinutes") ? ((Number) map.get("ttlMinutes")).longValue() : 60;

                Map<String, Object> params = map.containsKey("params") ? (Map<String, Object>) map.get("params") : Map.of();

                ApprovalRequest request = new ApprovalRequest(
                        requesterId, resource, action, riskLevel, impactSummary,
                        params, quorum, Duration.ofMinutes(ttlMinutes));

                PendingApproval approval = approvalGateway.create(request);

                notifier.notifyApprovalRequested(approval, List.of());

                log.info("[Gate4:HumanReview] Approval created: id={}, resource={}", approval.id(), resource);

                return ValidationResult.fail("HUMAN_REVIEW_REQUIRED",
                        "Approval required: " + approval.id());
            }
        }

        return ValidationResult.ok(output);
    }
}
