package ai.gameclaw.channels.wecom;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WeComCryptoTest {

    private static final String TOKEN = "test_token_123";
    private static final String CORP_ID = "ww1234567890abcdef";
    private static final SecureRandom RANDOM = new SecureRandom();

    private byte[] aesKey;
    private String encodingAesKey;
    private WeComCrypto crypto;

    @BeforeEach
    void setUp() {
        aesKey = new byte[32];
        RANDOM.nextBytes(aesKey);
        encodingAesKey = Base64.getEncoder().encodeToString(aesKey).substring(0, 43);
        crypto = new WeComCrypto(TOKEN, encodingAesKey, CORP_ID);
    }

    @Test
    void verifyReturnsTrueForValidSignature() {
        String timestamp = "1609459200";
        String nonce = "test_nonce";
        String body = "some_encrypted_body";

        String[] parts = {TOKEN, timestamp, nonce, body};
        Arrays.sort(parts);
        String joined = String.join("", parts);

        String expectedSignature = sha1Hex(joined);

        assertThat(crypto.verify(expectedSignature, timestamp, nonce, body)).isTrue();
    }

    @Test
    void verifyReturnsFalseForInvalidSignature() {
        assertThat(crypto.verify("invalid_signature", "1609459200", "nonce", "body")).isFalse();
    }

    @Test
    void decryptCorrectlyDecryptsValidMessage() throws Exception {
        String message = "<xml><Content>Hello WeCom</Content></xml>";
        String encrypted = encryptManually(message, CORP_ID);

        String decrypted = crypto.decrypt(encrypted);

        assertThat(decrypted).isEqualTo(message);
    }

    @Test
    void decryptThrowsOnCorpIdMismatch() throws Exception {
        String message = "<xml><Content>Hello</Content></xml>";
        String encrypted = encryptManually(message, "wrong_corp_id");

        assertThatThrownBy(() -> crypto.decrypt(encrypted))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CorpId mismatch");
    }

    @Test
    void encryptProducesValidXmlThatCanBeDecrypted() {
        String replyMsg = "<xml><Content>Reply message</Content></xml>";
        String timestamp = "1609459200";
        String nonce = "test_nonce";

        String encryptedXml = crypto.encrypt(replyMsg, timestamp, nonce);

        assertThat(encryptedXml).contains("<Encrypt>");
        assertThat(encryptedXml).contains("<MsgSignature>");
        assertThat(encryptedXml).contains("<TimeStamp>");
        assertThat(encryptedXml).contains("<Nonce>");

        String encryptedField = extractXmlField(encryptedXml, "Encrypt");
        String decrypted = crypto.decrypt(encryptedField);
        assertThat(decrypted).isEqualTo(replyMsg);
    }

    @Test
    void encryptSignatureCanBeVerified() {
        String replyMsg = "<xml><Content>Test</Content></xml>";
        String timestamp = "1609459200";
        String nonce = "nonce123";

        String encryptedXml = crypto.encrypt(replyMsg, timestamp, nonce);

        String encryptedField = extractXmlField(encryptedXml, "Encrypt");
        String signature = extractXmlField(encryptedXml, "MsgSignature");

        assertThat(crypto.verify(signature, timestamp, nonce, encryptedField)).isTrue();
    }

    private String encryptManually(String message, String corpId) throws Exception {
        byte[] randomBytes = new byte[16];
        RANDOM.nextBytes(randomBytes);
        byte[] msgBytes = message.getBytes(StandardCharsets.UTF_8);
        byte[] corpIdBytes = corpId.getBytes(StandardCharsets.UTF_8);

        ByteBuffer plainBuffer = ByteBuffer.allocate(16 + 4 + msgBytes.length + corpIdBytes.length);
        plainBuffer.put(randomBytes);
        plainBuffer.putInt(msgBytes.length);
        plainBuffer.put(msgBytes);
        plainBuffer.put(corpIdBytes);
        byte[] plaintext = plainBuffer.array();

        int blockSize = 32;
        int padLen = blockSize - (plaintext.length % blockSize);
        byte[] padded = Arrays.copyOf(plaintext, plaintext.length + padLen);
        Arrays.fill(padded, plaintext.length, padded.length, (byte) padLen);

        Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE,
                new SecretKeySpec(aesKey, "AES"),
                new IvParameterSpec(Arrays.copyOf(aesKey, 16)));
        byte[] encrypted = cipher.doFinal(padded);

        return Base64.getEncoder().encodeToString(encrypted);
    }

    private String sha1Hex(String input) {
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            byte[] digest = sha1.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String extractXmlField(String xml, String fieldName) {
        String cdataStart = "<" + fieldName + "><![CDATA[";
        String cdataEnd = "]]></" + fieldName + ">";
        int start = xml.indexOf(cdataStart);
        if (start >= 0) {
            start += cdataStart.length();
            int end = xml.indexOf(cdataEnd, start);
            if (end >= 0) return xml.substring(start, end);
        }
        String tagStart = "<" + fieldName + ">";
        String tagEnd = "</" + fieldName + ">";
        start = xml.indexOf(tagStart);
        if (start >= 0) {
            start += tagStart.length();
            int end = xml.indexOf(tagEnd, start);
            if (end >= 0) return xml.substring(start, end);
        }
        return null;
    }
}
