package ai.gameclaw.observability;

import ai.gameclaw.security.TenantContext;
import ai.gameclaw.security.TenantContextHolder;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Aspect
@Component
@ConditionalOnProperty(name = "gameclaw.audit.enabled", havingValue = "true", matchIfMissing = true)
public class AuditLogger {

    private static final Logger log = LoggerFactory.getLogger(AuditLogger.class);

    private final JdbcTemplate jdbc;

    public AuditLogger(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Around("@annotation(org.springframework.ai.tool.annotation.Tool)")
    public Object audit(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        String toolName = sig.getMethod().getName();

        TenantContext ctx = TenantContextHolder.tryGet().orElse(null);
        UUID tenantId = ctx != null ? ctx.tenantId() : null;
        UUID userId = ctx != null ? ctx.userId() : null;

        long start = System.nanoTime();
        String result = "success";
        String errorCode = null;
        String errorMessage = null;
        Object retVal = null;

        try {
            retVal = pjp.proceed();
        } catch (Throwable e) {
            result = "failed";
            errorCode = e.getClass().getSimpleName();
            errorMessage = e.getMessage();
            throw e;
        } finally {
            long latencyMs = (System.nanoTime() - start) / 1_000_000;
            try {
                jdbc.update(
                        "INSERT INTO audit_log (id, tenant_id, actor_id, action, resource, detail, created_at) " +
                                "VALUES (gen_random_uuid(), ?, ?, ?, ?, ?::jsonb, now())",
                        tenantId,
                        userId,
                        toolName,
                        "tool_invocation",
                        String.format("{\"result\":\"%s\",\"latencyMs\":%d,\"errorCode\":%s,\"errorMessage\":%s}",
                                result, latencyMs,
                                errorCode != null ? "\"" + errorCode + "\"" : "null",
                                errorMessage != null ? "\"" + escapeJson(errorMessage) + "\"" : "null")
                );
            } catch (Exception e) {
                log.warn("[AuditLogger] Failed to write audit_log: {}", e.getMessage());
            }
            log.info("[AuditLogger] tool={} tenant={} user={} result={} latency={}ms",
                    toolName, tenantId, userId, result, latencyMs);
        }
        return retVal;
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
