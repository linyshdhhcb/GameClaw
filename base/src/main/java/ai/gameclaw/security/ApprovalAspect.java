package ai.gameclaw.security;

import ai.gameclaw.governance.approval.ApprovalGateway;
import ai.gameclaw.governance.approval.ApprovalNotifier;
import ai.gameclaw.governance.approval.ApprovalRequest;
import ai.gameclaw.governance.approval.ApprovalRequiredException;
import ai.gameclaw.governance.approval.PendingApproval;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Aspect
@Component
@ConditionalOnBean(ApprovalGateway.class)
public class ApprovalAspect {

    private static final Logger log = LoggerFactory.getLogger(ApprovalAspect.class);

    private final ApprovalGateway approvalGateway;
    private final ApprovalNotifier notifier;
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public ApprovalAspect(ApprovalGateway approvalGateway, ApprovalNotifier notifier,
                          JdbcTemplate jdbc, ObjectProvider<ObjectMapper> objectMapperProvider) {
        this.approvalGateway = approvalGateway;
        this.notifier = notifier;
        this.jdbc = jdbc;
        this.objectMapper = objectMapperProvider.getIfAvailable(ObjectMapper::new);
    }

    @Around("@annotation(ai.gameclaw.security.RequireApproval)")
    public Object enforceApproval(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        RequireApproval annotation = sig.getMethod().getAnnotation(RequireApproval.class);
        TenantContext ctx = TenantContextHolder.require();

        String resource = pjp.getTarget().getClass().getSimpleName() + "." + sig.getMethod().getName();
        String action = "method_invocation";

        ApprovalRequest request = new ApprovalRequest(
                ctx.userId(),
                resource,
                action,
                RiskLevel.L3_PROJECT_WRITE,
                "Method requires approval",
                Map.of("quorum", annotation.quorum(), "approvers", annotation.approvers()),
                annotation.quorum(),
                Duration.ofMinutes(annotation.ttlMinutes())
        );

        PendingApproval approval = approvalGateway.create(request);

        notifier.notifyApprovalRequested(approval, java.util.List.of());

        writeAuditLog(ctx, resource, "approval_required",
                Map.of("approvalId", approval.id().toString(), "quorum", annotation.quorum()));

        log.info("[ApprovalAspect] Approval required for {}: approvalId={}", resource, approval.id());

        throw new ApprovalRequiredException(approval.id());
    }

    private void writeAuditLog(TenantContext ctx, String resource, String action, Map<String, Object> detail) {
        UUID tenantId = ctx != null ? ctx.tenantId() : null;
        UUID userId = ctx != null ? ctx.userId() : null;
        try {
            jdbc.update(
                    "INSERT INTO audit_log (id, tenant_id, actor_id, action, resource, detail, created_at) " +
                            "VALUES (gen_random_uuid(), ?, ?, ?, ?, ?::jsonb, now())",
                    tenantId, userId, action, resource, objectMapper.writeValueAsString(detail));
        } catch (Exception e) {
            log.warn("[ApprovalAspect] Failed to write audit_log: {}", e.getMessage());
        }
    }
}
