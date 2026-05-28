package ai.gameclaw.channels.wecom;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

public class WeComCrypto {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final String token;
    private final byte[] aesKey;
    private final String corpId;

    public WeComCrypto(String token, String encodingAesKey, String corpId) {
        this.token = token;
        this.aesKey = Base64.getDecoder().decode(encodingAesKey + "=");
        this.corpId = corpId;
    }

    public String decrypt(String encrypted) {
        try {
            byte[] encryptedBytes = Base64.getDecoder().decode(encrypted);
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE,
                    new SecretKeySpec(aesKey, "AES"),
                    new IvParameterSpec(Arrays.copyOf(aesKey, 16)));
            byte[] decrypted = cipher.doFinal(encryptedBytes);

            int pad = decrypted[decrypted.length - 1] & 0xFF;
            if (pad < 1 || pad > 32) {
                throw new IllegalArgumentException("Invalid PKCS#7 padding");
            }
            byte[] unpadded = Arrays.copyOfRange(decrypted, 0, decrypted.length - pad);

            ByteBuffer buffer = ByteBuffer.wrap(unpadded);
            byte[] random = new byte[16];
            buffer.get(random);
            int msgLen = buffer.getInt();
            byte[] msgBytes = new byte[msgLen];
            buffer.get(msgBytes);
            byte[] corpIdBytes = new byte[buffer.remaining()];
            buffer.get(corpIdBytes);

            String receivedCorpId = new String(corpIdBytes, StandardCharsets.UTF_8);
            if (!corpId.equals(receivedCorpId)) {
                throw new IllegalArgumentException("CorpId mismatch: expected=" + corpId + ", received=" + receivedCorpId);
            }

            return new String(msgBytes, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }

    public boolean verify(String signature, String timestamp, String nonce, String body) {
        String computed = computeSignature(timestamp, nonce, body);
        return MessageDigest.isEqual(
                computed.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8));
    }

    public String encrypt(String replyMsg, String timestamp, String nonce) {
        try {
            byte[] randomBytes = new byte[16];
            SECURE_RANDOM.nextBytes(randomBytes);
            byte[] msgBytes = replyMsg.getBytes(StandardCharsets.UTF_8);
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

            String encryptedBase64 = Base64.getEncoder().encodeToString(encrypted);
            String signature = computeSignature(timestamp, nonce, encryptedBase64);

            return "<xml>" +
                    "<Encrypt><![CDATA[" + encryptedBase64 + "]]></Encrypt>" +
                    "<MsgSignature><![CDATA[" + signature + "]]></MsgSignature>" +
                    "<TimeStamp>" + timestamp + "</TimeStamp>" +
                    "<Nonce><![CDATA[" + nonce + "]]></Nonce>" +
                    "</xml>";
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    private String computeSignature(String timestamp, String nonce, String body) {
        try {
            String[] parts = {token, timestamp, nonce, body};
            Arrays.sort(parts);
            String joined = String.join("", parts);
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            byte[] digest = sha1.digest(joined.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Signature computation failed", e);
        }
    }
}
