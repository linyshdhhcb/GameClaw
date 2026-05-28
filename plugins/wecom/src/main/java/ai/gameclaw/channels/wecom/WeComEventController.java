package ai.gameclaw.channels.wecom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@ConditionalOnProperty(prefix = "agent.channels.wecom", name = {"corp-id", "agent-id", "secret", "token", "encoding-aes-key"})
@RequestMapping("/api/wecom")
public class WeComEventController {

    private static final Logger log = LoggerFactory.getLogger(WeComEventController.class);

    private final WeComCrypto crypto;
    private final WeComChannel weComChannel;

    public WeComEventController(WeComCrypto crypto, WeComChannel weComChannel) {
        this.crypto = crypto;
        this.weComChannel = weComChannel;
    }

    @GetMapping("/event")
    public ResponseEntity<String> verifyUrl(
            @RequestParam("msg_signature") String msgSignature,
            @RequestParam("timestamp") String timestamp,
            @RequestParam("nonce") String nonce,
            @RequestParam("echostr") String echostr) {

        if (!crypto.verify(msgSignature, timestamp, nonce, echostr)) {
            log.warn("[WeCom] URL verification signature check failed");
            return ResponseEntity.status(401).build();
        }

        String decrypted = crypto.decrypt(echostr);
        return ResponseEntity.ok(decrypted);
    }

    @PostMapping("/event")
    public ResponseEntity<String> receive(
            @RequestParam(value = "msg_signature", required = false) String msgSignature,
            @RequestParam(value = "timestamp", required = false) String timestamp,
            @RequestParam(value = "nonce", required = false) String nonce,
            @RequestBody String body) {

        if (msgSignature != null && !crypto.verify(msgSignature, timestamp, nonce, extractEncrypt(body))) {
            log.warn("[WeCom] Callback signature verification failed");
            return ResponseEntity.status(401).build();
        }

        try {
            String encrypted = extractEncrypt(body);
            String decryptedXml = crypto.decrypt(encrypted);

            String msgType = extractXmlField(decryptedXml, "MsgType");
            String content = extractXmlField(decryptedXml, "Content");
            String fromUserName = extractXmlField(decryptedXml, "FromUserName");
            String toUserName = extractXmlField(decryptedXml, "ToUserName");

            if (fromUserName == null || fromUserName.isEmpty()) {
                return ResponseEntity.ok("success");
            }

            String corpId = toUserName;

            Thread.startVirtualThread(() ->
                    weComChannel.handleMessage(corpId, fromUserName, content, msgType));

            return ResponseEntity.ok("success");
        } catch (Exception e) {
            log.error("[WeCom] Failed to process callback: {}", e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    private String extractEncrypt(String xml) {
        return extractXmlField(xml, "Encrypt");
    }

    private String extractXmlField(String xml, String fieldName) {
        String cdataStart = "<" + fieldName + "><![CDATA[";
        String cdataEnd = "]]></" + fieldName + ">";
        int start = xml.indexOf(cdataStart);
        if (start >= 0) {
            start += cdataStart.length();
            int end = xml.indexOf(cdataEnd, start);
            if (end >= 0) {
                return xml.substring(start, end);
            }
        }

        String tagStart = "<" + fieldName + ">";
        String tagEnd = "</" + fieldName + ">";
        start = xml.indexOf(tagStart);
        if (start >= 0) {
            start += tagStart.length();
            int end = xml.indexOf(tagEnd, start);
            if (end >= 0) {
                return xml.substring(start, end);
            }
        }

        return null;
    }
}
