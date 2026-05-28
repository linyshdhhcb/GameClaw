package ai.gameclaw.channels.slack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SlackTenantRegistry {

    private static final Logger log = LoggerFactory.getLogger(SlackTenantRegistry.class);

    private final JdbcTemplate jdbc;
    private final ConcurrentHashMap<String, UUID> localCache = new ConcurrentHashMap<>();

    public SlackTenantRegistry(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public UUID resolveTenantId(String teamId) {
        if (teamId == null || teamId.isBlank()) return null;
        UUID cached = localCache.get(teamId);
        if (cached != null) return cached;

        if (jdbc == null) return null;

        try {
            String id = jdbc.queryForObject(
                    "SELECT tenant_id FROM slack_workspaces WHERE team_id = ?",
                    String.class, teamId);
            if (id != null) {
                UUID uuid = UUID.fromString(id);
                localCache.put(teamId, uuid);
                return uuid;
            }
        } catch (Exception e) {
            log.debug("[SlackTenantRegistry] DB lookup failed for team_id={}: {}", teamId, e.getMessage());
        }
        return null;
    }

    public void register(String teamId, UUID tenantId) {
        localCache.put(teamId, tenantId);
        if (jdbc == null) return;
        try {
            jdbc.update(
                    "INSERT INTO slack_workspaces (team_id, tenant_id) VALUES (?, ?) " +
                            "ON CONFLICT (team_id) DO UPDATE SET tenant_id = ?",
                    teamId, tenantId, tenantId);
        } catch (Exception e) {
            log.warn("[SlackTenantRegistry] Failed to persist tenant mapping: {}", e.getMessage());
        }
    }
}
