package ai.gameclaw.security.pii;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PiiMaskingTest {

    @Test
    void maskPhone() {
        assertThat(PiiMasking.maskPhone("13812345678")).isEqualTo("138****5678");
    }

    @Test
    void maskPhoneNull() {
        assertThat(PiiMasking.maskPhone(null)).isNull();
    }

    @Test
    void maskEmail() {
        assertThat(PiiMasking.maskEmail("user@example.com")).isEqualTo("u***@example.com");
    }

    @Test
    void maskEmailNull() {
        assertThat(PiiMasking.maskEmail(null)).isNull();
    }

    @Test
    void maskIdCard() {
        assertThat(PiiMasking.maskIdCard("110101199001011234")).isEqualTo("1101****1234");
    }

    @Test
    void maskIdCardNull() {
        assertThat(PiiMasking.maskIdCard(null)).isNull();
    }
}
