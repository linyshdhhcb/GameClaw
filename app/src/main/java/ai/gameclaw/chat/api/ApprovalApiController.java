package ai.gameclaw.chat.api;

import ai.gameclaw.governance.approval.ApprovalGateway;
import ai.gameclaw.governance.approval.PendingApproval;
import ai.gameclaw.governance.rollback.RollbackService;
import ai.gameclaw.security.Role;
import ai.gameclaw.security.TenantContext;
import ai.gameclaw.security.TenantContextHolder;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/approval")
public class ApprovalApiController {

    private final ApprovalGateway approvalGateway;
    private final RollbackService rollbackService;

    public ApprovalApiController(ApprovalGateway approvalGateway, RollbackService rollbackService) {
        this.approvalGateway = approvalGateway;
        this.rollbackService = rollbackService;
    }

    @GetMapping("/pending")
    public List<PendingApproval> pending() {
        TenantContext ctx = TenantContextHolder.tryGet().orElse(null);
        UUID tenantId = ctx != null ? ctx.tenantId() : null;
        if (tenantId == null) {
            return List.of();
        }
        return approvalGateway.findPending(tenantId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PendingApproval> getById(@PathVariable UUID id) {
        return approvalGateway.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<PendingApproval> approve(@PathVariable UUID id,
                                                   @RequestBody Map<String, String> body,
                                                   HttpSession session) {
        UUID userId = resolveUserId(session);
        String reason = body.getOrDefault("reason", "");
        PendingApproval approval = approvalGateway.approve(id, userId, reason);
        return ResponseEntity.ok(approval);
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<PendingApproval> reject(@PathVariable UUID id,
                                                  @RequestBody Map<String, String> body,
                                                  HttpSession session) {
        UUID userId = resolveUserId(session);
        String reason = body.getOrDefault("reason", "");
        PendingApproval approval = approvalGateway.reject(id, userId, reason);
        return ResponseEntity.ok(approval);
    }

    @PostMapping("/{id}/override")
    public ResponseEntity<?> override(@PathVariable UUID id,
                                      @RequestBody Map<String, String> body,
                                      HttpSession session) {
        TenantContext ctx = TenantContextHolder.tryGet().orElse(null);
        if (ctx == null || ctx.roles() == null ||
                !ctx.roles().contains(Role.ADMIN) && !ctx.roles().contains(Role.PLATFORM_ADMIN)) {
            return ResponseEntity.status(403).body(Map.of("error", "ADMIN role required for emergency override"));
        }

        UUID adminId = resolveUserId(session);
        String reason = body.get("reason");
        if (reason == null || reason.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Reason is required for emergency override"));
        }

        PendingApproval approval = approvalGateway.emergencyOverride(id, adminId, reason);
        return ResponseEntity.ok(approval);
    }

    @GetMapping("/resource/{resource}")
    public List<PendingApproval> byResource(@PathVariable String resource) {
        TenantContext ctx = TenantContextHolder.tryGet().orElse(null);
        UUID tenantId = ctx != null ? ctx.tenantId() : null;
        if (tenantId == null) {
            return List.of();
        }
        return approvalGateway.findByResource(tenantId, resource);
    }

    private UUID resolveUserId(HttpSession session) {
        TenantContext ctx = TenantContextHolder.tryGet().orElse(null);
        if (ctx != null && ctx.userId() != null) {
            return ctx.userId();
        }
        Object sessionUserId = session.getAttribute("userId");
        if (sessionUserId instanceof UUID uid) {
            return uid;
        }
        if (sessionUserId instanceof String s) {
            return UUID.fromString(s);
        }
        return UUID.randomUUID();
    }
}
