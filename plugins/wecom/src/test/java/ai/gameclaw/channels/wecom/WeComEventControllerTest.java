package ai.gameclaw.channels.wecom;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WeComEventControllerTest {

    private WeComCrypto crypto;
    private WeComChannel weComChannel;
    private WeComEventController controller;

    @BeforeEach
    void setUp() {
        crypto = mock(WeComCrypto.class);
        weComChannel = mock(WeComChannel.class);
        controller = new WeComEventController(crypto, weComChannel);
    }

    @Test
    void verifyUrlReturnsDecryptedEchostr() {
        when(crypto.verify("sig", "ts", "nonce", "echo123")).thenReturn(true);
        when(crypto.decrypt("echo123")).thenReturn("decrypted_echo");

        ResponseEntity<String> response = controller.verifyUrl("sig", "ts", "nonce", "echo123");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("decrypted_echo");
    }

    @Test
    void verifyUrlReturns401OnSignatureFailure() {
        when(crypto.verify("bad_sig", "ts", "nonce", "echo123")).thenReturn(false);

        ResponseEntity<String> response = controller.verifyUrl("bad_sig", "ts", "nonce", "echo123");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void postEventReturns401OnSignatureFailure() {
        when(crypto.verify("bad_sig", "ts", "nonce", "encrypted")).thenReturn(false);

        String body = "<xml><Encrypt><![CDATA[encrypted]]></Encrypt></xml>";
        ResponseEntity<String> response = controller.receive("bad_sig", "ts", "nonce", body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void postEventProcessesMessageSuccessfully() {
        when(crypto.verify("sig", "ts", "nonce", "encrypted")).thenReturn(true);
        String decryptedXml = "<xml>"
                + "<MsgType><![CDATA[text]]></MsgType>"
                + "<Content><![CDATA[hello]]></Content>"
                + "<FromUserName><![CDATA[user001]]></FromUserName>"
                + "<ToUserName><![CDATA[corp123]]></ToUserName>"
                + "</xml>";
        when(crypto.decrypt("encrypted")).thenReturn(decryptedXml);

        String body = "<xml><Encrypt><![CDATA[encrypted]]></Encrypt></xml>";
        ResponseEntity<String> response = controller.receive("sig", "ts", "nonce", body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("success");

        verify(weComChannel, timeout(2000)).handleMessage("corp123", "user001", "hello", "text");
    }

    @Test
    void postEventReturnsSuccessWhenNoFromUserName() {
        when(crypto.verify("sig", "ts", "nonce", "encrypted")).thenReturn(true);
        String decryptedXml = "<xml>"
                + "<MsgType><![CDATA[event]]></MsgType>"
                + "<ToUserName><![CDATA[corp123]]></ToUserName>"
                + "</xml>";
        when(crypto.decrypt("encrypted")).thenReturn(decryptedXml);

        String body = "<xml><Encrypt><![CDATA[encrypted]]></Encrypt></xml>";
        ResponseEntity<String> response = controller.receive("sig", "ts", "nonce", body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("success");
    }

    @Test
    void postEventReturns500OnCryptoException() {
        when(crypto.verify("sig", "ts", "nonce", "encrypted")).thenReturn(true);
        when(crypto.decrypt("encrypted")).thenThrow(new RuntimeException("Decryption failed"));

        String body = "<xml><Encrypt><![CDATA[encrypted]]></Encrypt></xml>";
        ResponseEntity<String> response = controller.receive("sig", "ts", "nonce", body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void postEventWithoutSignatureProcessesMessage() {
        String decryptedXml = "<xml>"
                + "<MsgType><![CDATA[text]]></MsgType>"
                + "<Content><![CDATA[hello]]></Content>"
                + "<FromUserName><![CDATA[user001]]></FromUserName>"
                + "<ToUserName><![CDATA[corp123]]></ToUserName>"
                + "</xml>";
        when(crypto.decrypt("encrypted")).thenReturn(decryptedXml);

        String body = "<xml><Encrypt><![CDATA[encrypted]]></Encrypt></xml>";
        ResponseEntity<String> response = controller.receive(null, "ts", "nonce", body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
