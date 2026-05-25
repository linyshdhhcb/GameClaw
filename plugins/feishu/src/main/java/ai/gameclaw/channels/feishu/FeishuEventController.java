package ai.gameclaw.channels.feishu;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@ConditionalOnProperty(prefix = "agent.channels.feishu", name = {"app-id", "app-secret"})
public class FeishuEventController {

    private static final Logger log = LoggerFactory.getLogger(FeishuEventController.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String verificationToken;
    private final String encryptKey;
    private final NonceCache nonceCache;
    private final FeishuChannel feishuChannel;

    public FeishuEventController(
            @Value("${agent.channels.feishu.verification-token:}") String verificationToken,
            @Value("${agent.channels.feishu.encrypt-key:}") String encryptKey,
            NonceCache nonceCache, FeishuChannel feishuChannel) {
        this.verificationToken = verificationToken;
        this.encryptKey = encryptKey;
        this.nonceCache = nonceCache;
        this.feishuChannel = feishuChannel;
    }

    @PostMapping("/api/feishu/event")
    public ResponseEntity<?> receive(
            @RequestHeader(value = "X-Lark-Signature", required = false) String signature,
            @RequestHeader(value = "X-Lark-Request-Timestamp", required = false) String timestamp,
            @RequestHeader(value = "X-Lark-Request-Nonce", required = false) String nonce,
            @RequestBody byte[] body) {

        if (timestamp != null) {
            long now = Instant.now().getEpochSecond();
            try {
                long ts = Long.parseLong(timestamp);
                if (Math.abs(now - ts) > 300) {
                    log.warn("[Feishu] Timestamp expired: ts={} now={}", ts, now);
                    return ResponseEntity.status(401).build();
                }
            } catch (NumberFormatException e) {
                return ResponseEntity.status(401).build();
            }
        }

        if (nonce != null && !nonceCache.acquire(nonce)) {
            log.warn("[Feishu] Nonce replay detected: {}", nonce);
            return ResponseEntity.status(401).build();
        }

        try {
            JsonNode root = MAPPER.readTree(body);

            if ("url_verification".equals(root.path("type").asText())) {
                String challenge = root.path("challenge").asText();
                return ResponseEntity.ok(java.util.Map.of("challenge", challenge));
            }

            if (!"event_callback".equals(root.path("type").asText())) {
                return ResponseEntity.ok().build();
            }

            FeishuEvent event = FeishuEventParser.parse(body);
            if (event.text() == null || event.text().isBlank()) {
                return ResponseEntity.ok().build();
            }

            Thread.startVirtualThread(() -> feishuChannel.handleEvent(event));
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("[Feishu] Failed to process event: {}", e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }
}
