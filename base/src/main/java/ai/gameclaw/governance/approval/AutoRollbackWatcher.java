package ai.gameclaw.governance.approval;

import ai.gameclaw.observability.AuditLogger;
import ai.gameclaw.governance.rollback.RollbackKind;
import ai.gameclaw.governance.rollback.RollbackService;
import ai.gameclaw.governance.rollback.RollbackSnapshot;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "gameclaw.governance.rollback.auto.enabled", havingValue = "true")
public class AutoRollbackWatcher {

    private static final Logger log = LoggerFactory.getLogger(AutoRollbackWatcher.class);

    private final RollbackService rollbackService;
    private final JdbcTemplate jdbc;
    private final ObjectProvider<AuditLogger> auditLoggerProvider;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public AutoRollbackWatcher(RollbackService rollbackService, JdbcTemplate jdbc,
                               ObjectProvider<AuditLogger> auditLoggerProvider, ObjectProvider<ObjectMapper> objectMapperProvider) {
        this.rollbackService = rollbackService;
        this.jdbc = jdbc;
        this.auditLoggerProvider = auditLoggerProvider;
        this.objectMapper = objectMapperProvider.getIfAvailable(ObjectMapper::new);
        this.httpClient = HttpClient.newHttpClient();
    }

    @Scheduled(fixedDelay = 30000)
    public void watch() {
        try {
            List<RollbackSnapshot> recentDeploys = findRecentK8sDeploys();
            for (RollbackSnapshot snapshot : recentDeploys) {
                checkAndAutoRollback(snapshot);
            }
        } catch (Exception e) {
            log.error("[AutoRollbackWatcher] Error during watch cycle: {}", e.getMessage());
        }
    }

    private List<RollbackSnapshot> findRecentK8sDeploys() {
        return jdbc.query(
                "SELECT rs.* FROM rollback_snapshots rs " +
                        "WHERE rs.snapshot::jsonb->>'kind' = 'K8S_DEPLOY' " +
                        "AND rs.created_at > now() - interval '10 minutes' " +
                        "ORDER BY rs.created_at DESC",
                (rs, rowNum) -> {
                    try {
                        String snapshotJson = rs.getString("snapshot");
                        Map<String, Object> snapshotMap = objectMapper.readValue(snapshotJson, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
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
                    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    private void checkAndAutoRollback(RollbackSnapshot snapshot) {
        if (snapshot.kind() != RollbackKind.K8S_DEPLOY) {
            return;
        }

        long ageSeconds = Instant.now().getEpochSecond() - snapshot.createdAt().getEpochSecond();
        if (ageSeconds > 600) {
            return;
        }

        try {
            double errorRate = queryPrometheus5xxRate(snapshot);
            if (errorRate > 0.05) {
                log.warn("[AutoRollbackWatcher] 5xx error rate {}% exceeds threshold for snapshot {}, triggering auto-rollback",
                        errorRate * 100, snapshot.id());

                rollbackService.rollback(snapshot.id());

                writeAuditLog(snapshot.tenantId(), null, "auto_rollback_triggered", snapshot.resource(),
                        Map.of("snapshotId", snapshot.id().toString(), "errorRate", errorRate, "kind", "K8S_DEPLOY"));
            }
        } catch (Exception e) {
            log.warn("[AutoRollbackWatcher] Failed to check error rate for snapshot {}: {}", snapshot.id(), e.getMessage());
        }
    }

    private double queryPrometheus5xxRate(RollbackSnapshot snapshot) {
        try {
            String query = "sum(rate(http_requests_total{status=~\"5..\"}[5m])) / sum(rate(http_requests_total[5m]))";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:9090/api/v1/query?query=" +
                            java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8)))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode result = root.at("/data/result");
            if (result.isArray() && !result.isEmpty()) {
                JsonNode value = result.get(0).at("/value/1");
                if (value.isTextual()) {
                    return Double.parseDouble(value.asText());
                }
            }
            return 0.0;
        } catch (Exception e) {
            log.debug("[AutoRollbackWatcher] Prometheus query failed: {}", e.getMessage());
            return 0.0;
        }
    }

    private void writeAuditLog(UUID tenantId, UUID actorId, String action, String resource, Map<String, Object> detail) {
        try {
            jdbc.update(
                    "INSERT INTO audit_log (id, tenant_id, actor_id, action, resource, detail, created_at) " +
                            "VALUES (gen_random_uuid(), ?, ?, ?, ?, ?::jsonb, now())",
                    tenantId, actorId, action, resource, objectMapper.writeValueAsString(detail));
        } catch (Exception e) {
            log.warn("[AutoRollbackWatcher] Failed to write audit_log: {}", e.getMessage());
        }
    }
}
