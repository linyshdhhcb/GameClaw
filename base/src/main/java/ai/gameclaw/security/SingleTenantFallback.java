package ai.gameclaw.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@ConditionalOnProperty(name = "gameclaw.multi-tenancy.enabled", havingValue = "false", matchIfMissing = true)
public class SingleTenantFallback {

    private static final UUID DEFAULT_TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    public TenantContext defaultContext() {
        return TenantContext.of(DEFAULT_TENANT_ID);
    }

    public UUID getDefaultTenantId() {
        return DEFAULT_TENANT_ID;
    }
}
