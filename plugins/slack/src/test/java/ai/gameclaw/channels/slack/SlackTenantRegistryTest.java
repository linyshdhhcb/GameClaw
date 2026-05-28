package ai.gameclaw.channels.slack;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SlackTenantRegistryTest {

    private JdbcTemplate jdbc;
    private SlackTenantRegistry registry;

    @BeforeEach
    void setUp() {
        jdbc = mock(JdbcTemplate.class);
        registry = new SlackTenantRegistry(jdbc);
    }

    @Test
    void resolveTenantIdReturnsUuidFromDatabase() {
        UUID expected = UUID.randomUUID();
        when(jdbc.queryForObject(anyString(), eq(String.class), any(Object[].class)))
                .thenReturn(expected.toString());

        UUID result = registry.resolveTenantId("T12345");

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void resolveTenantIdReturnsNullWhenTeamIdIsNull() {
        UUID result = registry.resolveTenantId(null);

        assertThat(result).isNull();
    }

    @Test
    void resolveTenantIdReturnsNullWhenTeamIdIsBlank() {
        UUID result = registry.resolveTenantId("  ");

        assertThat(result).isNull();
    }

    @Test
    void resolveTenantIdReturnsNullWhenDatabaseHasNoMapping() {
        when(jdbc.queryForObject(anyString(), eq(String.class), any(Object[].class)))
                .thenReturn(null);

        UUID result = registry.resolveTenantId("T_UNKNOWN");

        assertThat(result).isNull();
    }

    @Test
    void resolveTenantIdReturnsNullOnDatabaseException() {
        when(jdbc.queryForObject(anyString(), eq(String.class), any(Object[].class)))
                .thenThrow(new RuntimeException("DB error"));

        UUID result = registry.resolveTenantId("T_ERROR");

        assertThat(result).isNull();
    }

    @Test
    void resolveTenantIdCachesResult() {
        UUID expected = UUID.randomUUID();
        when(jdbc.queryForObject(anyString(), eq(String.class), any(Object[].class)))
                .thenReturn(expected.toString());

        UUID first = registry.resolveTenantId("T_CACHE");
        UUID second = registry.resolveTenantId("T_CACHE");

        assertThat(first).isEqualTo(expected);
        assertThat(second).isEqualTo(expected);
    }

    @Test
    void resolveTenantIdReturnsNullWhenJdbcIsNull() {
        SlackTenantRegistry noJdbcRegistry = new SlackTenantRegistry(null);

        UUID result = noJdbcRegistry.resolveTenantId("T12345");

        assertThat(result).isNull();
    }
}
