package ai.gameclaw.channels.feishu;

import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class FeishuSignTest {

    @Test
    void verifyValidSignature() throws Exception {
        String secret = "test_secret";
        String timestamp = "1234567890";
        String nonce = "abc123";
        byte[] body = "{\"event\":{}}".getBytes(StandardCharsets.UTF_8);

        String content = timestamp + "\n" + nonce + "\n" + new String(body, StandardCharsets.UTF_8);
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
        String signature = Base64.getEncoder().encodeToString(hash);

        assertThat(FeishuSign.verify(signature, timestamp, nonce, secret, body)).isTrue();
    }

    @Test
    void verifyInvalidSignatureReturnsFalse() {
        assertThat(FeishuSign.verify("invalid", "123", "nonce", "secret", "body".getBytes())).isFalse();
    }
}
