package ai.gameclaw.mcp.server.clickhouse;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SqlSafetyValidatorTest {

    private final SqlSafetyValidator validator = new SqlSafetyValidator();

    @Test
    void allowsSimpleSelect() {
        assertThatCode(() -> validator.assertSelectOnly("SELECT * FROM events LIMIT 10"))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsInsert() {
        assertThatThrownBy(() -> validator.assertSelectOnly("INSERT INTO events VALUES (1, 'test')"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Only SELECT allowed");
    }

    @Test
    void rejectsDelete() {
        assertThatThrownBy(() -> validator.assertSelectOnly("DELETE FROM events WHERE 1=1"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Only SELECT allowed");
    }

    @Test
    void rejectsDangerousPattern() {
        assertThatThrownBy(() -> validator.assertSelectOnly("SELECT * FROM system.users"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Dangerous SQL pattern");
    }

    @Test
    void rejectsUnparseableSql() {
        assertThatThrownBy(() -> validator.assertSelectOnly("NOT SQL AT ALL !!!"))
                .isInstanceOf(SecurityException.class);
    }
}
