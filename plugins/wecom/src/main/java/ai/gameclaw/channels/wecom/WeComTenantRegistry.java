package ai.gameclaw.channels.wecom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WeComTenantRegistry {

    private static final Logger log = LoggerFactory.getLogger(WeComTenantRegistry.class);

    private final JdbcTemplate jdbc;
    private final ConcurrentHashMap<String, UUID> localCache = new ConcurrentHashMap<>();

    public WeComTenantRegistry(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public UUID resolveTenantId(String corpId) {
        if (corpId == null || corpId.isBlank()) return null;
        UUID cached = localCache.get(corpId);
        if (cached != null) return cached;

        if (jdbc == null) return null;

        try {
            String id = jdbc.queryForObject(
                    "SELECT tenant_id FROM wecom_tenants WHERE corp_id = ?",
                    String.class, corpId);
            if (id != null) {
                UUID uuid = UUID.fromString(id);
                localCache.put(corpId, uuid);
                return uuid;
            }
        } catch (Exception e) {
            log.debug("[WeComTenantRegistry] DB lookup failed for corp_id={}: {}", corpId, e.getMessage());
        }
        return null;
    }

    public void register(String corpId, UUID tenantId) {
        localCache.put(corpId, tenantId);
        if (jdbc == null) return;
        try {
            jdbc.update(
                    "INSERT INTO wecom_tenants (corp_id, tenant_id) VALUES (?, ?) " +
                            "ON CONFLICT (corp_id) DO UPDATE SET tenant_id = ?",
                    corpId, tenantId, tenantId);
        } catch (Exception e) {
            log.warn("[WeComTenantRegistry] Failed to persist tenant mapping: {}", e.getMessage());
        }
    }
}
