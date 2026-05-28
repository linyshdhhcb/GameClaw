package ai.gameclaw.governance.rollback;

import ai.gameclaw.observability.AuditLogger;
import ai.gameclaw.security.TenantContext;
import ai.gameclaw.security.TenantContextHolder;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "gameclaw.governance.rollback.enabled", havingValue = "true", matchIfMissing = true)
public class RollbackService {

    private static final Logger log = LoggerFactory.getLogger(RollbackService.class);

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<AuditLogger> auditLoggerProvider;
    private final FilesystemRestorer filesystemRestorer;

    public RollbackService(JdbcTemplate jdbc, ObjectProvider<ObjectMapper> objectMapperProvider,
                           ObjectProvider<AuditLogger> auditLoggerProvider,
                           FilesystemRestorer filesystemRestorer) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapperProvider.getIfAvailable(ObjectMapper::new);
        this.auditLoggerProvider = auditLoggerProvider;
        this.filesystemRestorer = filesystemRestorer;
    }

    public RollbackSnapshot createSnapshot(UUID approvalId, String resource, RollbackKind kind, Map<String, Object> data) {
        UUID id = UUID.randomUUID();
        TenantContext ctx = TenantContextHolder.tryGet().orElse(null);
        UUID tenantId = ctx != null ? ctx.tenantId() : null;
        Instant now = Instant.now();

        jdbc.update(
                "INSERT INTO rollback_snapshots (id, tenant_id, approval_id, resource, snapshot, created_at) " +
                        "VALUES (?, ?, ?, ?, ?::jsonb, ?)",
                id, tenantId, approvalId, resource,
                toJson(Map.of("kind", kind.name(), "data", data)),
                Timestamp.from(now));

        return new RollbackSnapshot(id, tenantId, approvalId, resource, kind, data, now);
    }

    public Optional<RollbackSnapshot> findSnapshot(UUID id) {
        List<RollbackSnapshot> results = jdbc.query(
                "SELECT * FROM rollback_snapshots WHERE id = ?",
                (rs, rowNum) -> mapRow(rs), id);
        return results.stream().findFirst();
    }

    public void rollback(UUID snapshotId) {
        RollbackSnapshot snapshot = findSnapshot(snapshotId)
                .orElseThrow(() -> new IllegalArgumentException("Rollback snapshot not found: " + snapshotId));

        TenantContext ctx = TenantContextHolder.tryGet().orElse(null);
        UUID tenantId = ctx != null ? ctx.tenantId() : null;
        UUID actorId = ctx != null ? ctx.userId() : null;

        switch (snapshot.kind()) {
            case FILE_WRITE -> {
                String filePath = (String) snapshot.snapshotData().get("filePath");
                String originalContent = (String) snapshot.snapshotData().get("originalContent");
                filesystemRestorer.restore(filePath, originalContent);
            }
            case GIT_COMMIT -> {
                String commitSha = (String) snapshot.snapshotData().get("commitSha");
                executeGitRevert(commitSha);
            }
            case K8S_DEPLOY -> {
                String revision = (String) snapshot.snapshotData().get("revision");
                executeK8sRollback(revision);
            }
            case DB_MIGRATION -> {
                String reverseSql = (String) snapshot.snapshotData().get("reverseSql");
                jdbc.execute(reverseSql);
            }
        }

        writeAuditLog(tenantId, actorId, "rollback_executed", snapshot.resource(),
                Map.of("snapshotId", snapshotId.toString(), "kind", snapshot.kind().name()));

        log.info("[RollbackService] Executed rollback: snapshotId={}, kind={}, resource={}",
                snapshotId, snapshot.kind(), snapshot.resource());
    }

    public List<RollbackSnapshot> findSnapshotsByApproval(UUID approvalId) {
        return jdbc.query(
                "SELECT * FROM rollback_snapshots WHERE approval_id = ?",
                (rs, rowNum) -> mapRow(rs), approvalId);
    }

    private void executeGitRevert(String commitSha) {
        try {
            ProcessBuilder pb = new ProcessBuilder("git", "revert", "--no-edit", commitSha);
            pb.inheritIO();
            Process process = pb.start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("git revert failed with exit code: " + exitCode);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute git revert: " + e.getMessage(), e);
        }
    }

    private void executeK8sRollback(String revision) {
        try {
            ProcessBuilder pb = new ProcessBuilder("kubectl", "rollout", "undo", revision);
            pb.inheritIO();
            Process process = pb.start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("kubectl rollout undo failed with exit code: " + exitCode);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute kubectl rollback: " + e.getMessage(), e);
        }
    }

    private RollbackSnapshot mapRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        String snapshotJson = rs.getString("snapshot");
        Map<String, Object> snapshotMap = fromJson(snapshotJson, new TypeReference<Map<String, Object>>() {});
        RollbackKind kind = RollbackKind.valueOf((String) snapshotMap.get("kind"));

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) snapshotMap.get("data");

        return new RollbackSnapshot(
                rs.getObject("id", UUID.class),
                rs.getObject("tenant_id", UUID.class),
                rs.getObject("approval_id", UUID.class),
                rs.getString("resource"),
                kind,
                data != null ? data : Map.of(),
                rs.getTimestamp("created_at").toInstant()
        );
    }

    private void writeAuditLog(UUID tenantId, UUID actorId, String action, String resource, Map<String, Object> detail) {
        try {
            jdbc.update(
                    "INSERT INTO audit_log (id, tenant_id, actor_id, action, resource, detail, created_at) " +
                            "VALUES (gen_random_uuid(), ?, ?, ?, ?, ?::jsonb, now())",
                    tenantId, actorId, action, resource, toJson(detail));
        } catch (Exception e) {
            log.warn("[RollbackService] Failed to write audit_log: {}", e.getMessage());
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize JSON", e);
        }
    }

    private <T> T fromJson(String value, TypeReference<T> typeRef) {
        try {
            if (value == null || value.isBlank()) {
                return null;
            }
            return objectMapper.readValue(value, typeRef);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize JSON", e);
        }
    }
}
