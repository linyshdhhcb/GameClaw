package ai.gameclaw.channels.feishu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FeishuTenantRegistry {

    private static final Logger log = LoggerFactory.getLogger(FeishuTenantRegistry.class);

    private final JdbcTemplate jdbc;
    private final ConcurrentHashMap<String, UUID> localCache = new ConcurrentHashMap<>();

    public FeishuTenantRegistry(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public UUID resolveTenantId(String tenantKey) {
        if (tenantKey == null || tenantKey.isBlank()) return null;
        UUID cached = localCache.get(tenantKey);
        if (cached != null) return cached;

        try {
            String id = jdbc.queryForObject(
                    "SELECT tenant_id FROM feishu_tenants WHERE tenant_key = ?",
                    String.class, tenantKey);
            if (id != null) {
                UUID uuid = UUID.fromString(id);
                localCache.put(tenantKey, uuid);
                return uuid;
            }
        } catch (Exception e) {
            log.debug("[FeishuTenantRegistry] DB lookup failed for tenant_key={}: {}", tenantKey, e.getMessage());
        }
        return null;
    }

    public void register(String tenantKey, UUID tenantId) {
        localCache.put(tenantKey, tenantId);
        try {
            jdbc.update(
                    "INSERT INTO feishu_tenants (tenant_key, tenant_id) VALUES (?, ?) " +
                            "ON CONFLICT (tenant_key) DO UPDATE SET tenant_id = ?",
                    tenantKey, tenantId, tenantId);
        } catch (Exception e) {
            log.warn("[FeishuTenantRegistry] Failed to persist tenant mapping: {}", e.getMessage());
        }
    }
}
