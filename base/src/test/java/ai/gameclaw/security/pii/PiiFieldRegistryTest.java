package ai.gameclaw.security.pii;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PiiFieldRegistryTest {

    @Test
    void classifiesPhone() {
        assertThat(PiiFieldRegistry.classify("phone")).isEqualTo(PiiType.PHONE);
        assertThat(PiiFieldRegistry.classify("mobile")).isEqualTo(PiiType.PHONE);
        assertThat(PiiFieldRegistry.classify("user_phone")).isEqualTo(PiiType.PHONE);
    }

    @Test
    void classifiesEmail() {
        assertThat(PiiFieldRegistry.classify("email")).isEqualTo(PiiType.EMAIL);
        assertThat(PiiFieldRegistry.classify("user_email")).isEqualTo(PiiType.EMAIL);
    }

    @Test
    void classifiesIdCard() {
        assertThat(PiiFieldRegistry.classify("id_card")).isEqualTo(PiiType.ID_CARD);
        assertThat(PiiFieldRegistry.classify("identity")).isEqualTo(PiiType.ID_CARD);
    }

    @Test
    void classifiesIp() {
        assertThat(PiiFieldRegistry.classify("ip")).isEqualTo(PiiType.IP);
        assertThat(PiiFieldRegistry.classify("client_ip")).isEqualTo(PiiType.IP);
        assertThat(PiiFieldRegistry.classify("login_ip")).isEqualTo(PiiType.IP);
    }

    @Test
    void classifiesUserId() {
        assertThat(PiiFieldRegistry.classify("user_id")).isEqualTo(PiiType.USER_ID);
        assertThat(PiiFieldRegistry.classify("player_id")).isEqualTo(PiiType.USER_ID);
    }

    @Test
    void returnsNoneForUnknown() {
        assertThat(PiiFieldRegistry.classify("event_name")).isEqualTo(PiiType.NONE);
        assertThat(PiiFieldRegistry.classify("count")).isEqualTo(PiiType.NONE);
        assertThat(PiiFieldRegistry.classify(null)).isEqualTo(PiiType.NONE);
    }
}
