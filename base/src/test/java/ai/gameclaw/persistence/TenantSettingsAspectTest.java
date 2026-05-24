package ai.gameclaw.persistence;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TenantSettingsAspectTest {

    @Test
    void sanitizeUuidAcceptsValidUuid() {
        TenantSettingsAspect aspect = new TenantSettingsAspect(null);
        UUID uuid = UUID.fromString("11111111-1111-1111-1111-111111111111");
        String result = invokeSanitize(aspect, uuid);
        assertThat(result).isEqualTo("11111111-1111-1111-1111-111111111111");
    }

    @Test
    void sanitizeUuidAcceptsAllZeroUuid() {
        TenantSettingsAspect aspect = new TenantSettingsAspect(null);
        UUID uuid = UUID.fromString("00000000-0000-0000-0000-000000000000");
        String result = invokeSanitize(aspect, uuid);
        assertThat(result).isEqualTo("00000000-0000-0000-0000-000000000000");
    }

    @Test
    void sanitizeUuidAcceptsRandomUuid() {
        TenantSettingsAspect aspect = new TenantSettingsAspect(null);
        UUID uuid = UUID.randomUUID();
        String result = invokeSanitize(aspect, uuid);
        assertThat(result).isEqualTo(uuid.toString());
    }

    private String invokeSanitize(TenantSettingsAspect aspect, UUID uuid) {
        try {
            var method = TenantSettingsAspect.class.getDeclaredMethod("sanitizeUuid", UUID.class);
            method.setAccessible(true);
            return (String) method.invoke(aspect, uuid);
        } catch (Exception e) {
            if (e.getCause() instanceof RuntimeException re) throw re;
            throw new RuntimeException(e);
        }
    }
}
