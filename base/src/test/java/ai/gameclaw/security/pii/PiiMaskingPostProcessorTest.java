package ai.gameclaw.security.pii;

import ai.gameclaw.security.Role;
import ai.gameclaw.security.TenantContext;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PiiMaskingPostProcessorTest {

    private static final UUID T1 = UUID.randomUUID();
    private static final UUID P1 = UUID.randomUUID();
    private static final UUID U1 = UUID.randomUUID();

    private static Map<String, Object> mapOf(Object... kv) {
        var m = new HashMap<String, Object>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    @Test
    void masksPhoneField() {
        var rows = List.<Map<String, Object>>of(mapOf("phone", "13812345678", "name", "Alice"));
        var ctx = TenantContext.of(T1, P1, U1, Set.of(Role.PLANNER));
        var result = PiiMaskingPostProcessor.mask(rows, ctx);
        assertThat(result.getFirst().get("phone")).isEqualTo("138****5678");
        assertThat(result.getFirst().get("name")).isEqualTo("Alice");
    }

    @Test
    void masksEmailField() {
        var rows = List.<Map<String, Object>>of(mapOf("email", "test@example.com", "score", 100));
        var ctx = TenantContext.of(T1, P1, U1, Set.of(Role.PLANNER));
        var result = PiiMaskingPostProcessor.mask(rows, ctx);
        assertThat(result.getFirst().get("email")).isEqualTo("t***@example.com");
        assertThat(result.getFirst().get("score")).isEqualTo(100);
    }

    @Test
    void masksIdCardField() {
        var rows = List.<Map<String, Object>>of(mapOf("id_card", "110101199001011234"));
        var ctx = TenantContext.of(T1, P1, U1, Set.of(Role.PLANNER));
        var result = PiiMaskingPostProcessor.mask(rows, ctx);
        assertThat(result.getFirst().get("id_card")).isEqualTo("1101****1234");
    }

    @Test
    void masksIpField() {
        var rows = List.<Map<String, Object>>of(mapOf("ip", "192.168.1.100"));
        var ctx = TenantContext.of(T1, P1, U1, Set.of(Role.PLANNER));
        var result = PiiMaskingPostProcessor.mask(rows, ctx);
        assertThat(result.getFirst().get("ip")).isEqualTo("192.168.*.*");
    }

    @Test
    void masksUserIdField() {
        var rows = List.<Map<String, Object>>of(mapOf("user_id", "abc123xyz"));
        var ctx = TenantContext.of(T1, P1, U1, Set.of(Role.PLANNER));
        var result = PiiMaskingPostProcessor.mask(rows, ctx);
        assertThat(result.getFirst().get("user_id")).isEqualTo("ab****yz");
    }

    @Test
    void adminSeesFullPii() {
        var rows = List.<Map<String, Object>>of(mapOf("phone", "13812345678", "email", "test@example.com"));
        var ctx = TenantContext.of(T1, P1, U1, Set.of(Role.ADMIN));
        var result = PiiMaskingPostProcessor.mask(rows, ctx);
        assertThat(result.getFirst().get("phone")).isEqualTo("13812345678");
        assertThat(result.getFirst().get("email")).isEqualTo("test@example.com");
    }

    @Test
    void dataAnalystSeesFullPii() {
        var rows = List.<Map<String, Object>>of(mapOf("phone", "13812345678"));
        var ctx = TenantContext.of(T1, P1, U1, Set.of(Role.DATA_ANALYST));
        var result = PiiMaskingPostProcessor.mask(rows, ctx);
        assertThat(result.getFirst().get("phone")).isEqualTo("13812345678");
    }

    @Test
    void nonSensitiveFieldNotMasked() {
        var rows = List.<Map<String, Object>>of(mapOf("event_name", "login", "count", 42));
        var ctx = TenantContext.of(T1, P1, U1, Set.of(Role.PLANNER));
        var result = PiiMaskingPostProcessor.mask(rows, ctx);
        assertThat(result.getFirst().get("event_name")).isEqualTo("login");
        assertThat(result.getFirst().get("count")).isEqualTo(42);
    }
}
