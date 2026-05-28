package ai.gameclaw.channels.wecom;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class WeComTenantRegistryTest {

    private JdbcTemplate jdbc;
    private WeComTenantRegistry registry;

    @BeforeEach
    void setUp() {
        jdbc = mock(JdbcTemplate.class);
        registry = new WeComTenantRegistry(jdbc);
    }

    @Test
    void resolveTenantIdReturnsNullForNullCorpId() {
        assertThat(registry.resolveTenantId(null)).isNull();
    }

    @Test
    void resolveTenantIdReturnsNullForBlankCorpId() {
        assertThat(registry.resolveTenantId("")).isNull();
        assertThat(registry.resolveTenantId("   ")).isNull();
    }

    @Test
    void resolveTenantIdReturnsUuidFromDatabase() {
        UUID expected = UUID.randomUUID();
        when(jdbc.queryForObject(eq("SELECT tenant_id FROM wecom_tenants WHERE corp_id = ?"),
                eq(String.class), eq("ww123456")))
                .thenReturn(expected.toString());

        UUID result = registry.resolveTenantId("ww123456");

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void resolveTenantIdReturnsNullWhenNotFound() {
        when(jdbc.queryForObject(anyString(), eq(String.class), anyString()))
                .thenReturn(null);

        assertThat(registry.resolveTenantId("ww_notfound")).isNull();
    }

    @Test
    void resolveTenantIdReturnsNullOnDatabaseException() {
        when(jdbc.queryForObject(anyString(), eq(String.class), anyString()))
                .thenThrow(new RuntimeException("DB error"));

        assertThat(registry.resolveTenantId("ww_error")).isNull();
    }

    @Test
    void resolveTenantIdCachesResult() {
        UUID expected = UUID.randomUUID();
        when(jdbc.queryForObject(anyString(), eq(String.class), eq("ww_cached")))
                .thenReturn(expected.toString());

        UUID first = registry.resolveTenantId("ww_cached");
        UUID second = registry.resolveTenantId("ww_cached");

        assertThat(first).isEqualTo(expected);
        assertThat(second).isEqualTo(expected);
        verify(jdbc, times(1)).queryForObject(anyString(), eq(String.class), eq("ww_cached"));
    }

    @Test
    void registerPersistsToDatabase() {
        UUID tenantId = UUID.randomUUID();

        registry.register("ww_new", tenantId);

        verify(jdbc).update(
                eq("INSERT INTO wecom_tenants (corp_id, tenant_id) VALUES (?, ?) ON CONFLICT (corp_id) DO UPDATE SET tenant_id = ?"),
                eq("ww_new"), eq(tenantId), eq(tenantId));
    }

    @Test
    void registerCachesLocally() {
        UUID tenantId = UUID.randomUUID();

        registry.register("ww_local", tenantId);

        UUID result = registry.resolveTenantId("ww_local");
        assertThat(result).isEqualTo(tenantId);
        verify(jdbc, never()).queryForObject(anyString(), eq(String.class), anyString());
    }

    @Test
    void resolveTenantIdReturnsNullWhenJdbcIsNull() {
        WeComTenantRegistry noJdbcRegistry = new WeComTenantRegistry(null);

        assertThat(noJdbcRegistry.resolveTenantId("ww_test")).isNull();
    }
}
