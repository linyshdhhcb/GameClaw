package ai.gameclaw.channels.feishu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.Map;

public class FeishuApiClient {

    private static final Logger log = LoggerFactory.getLogger(FeishuApiClient.class);

    private final String appId;
    private final String appSecret;
    private final RestClient restClient;
    private volatile String tenantAccessToken;
    private volatile long tokenExpiresAt;

    public FeishuApiClient(String appId, String appSecret) {
        this.appId = appId;
        this.appSecret = appSecret;
        this.restClient = RestClient.builder()
                .baseUrl("https://open.feishu.cn/open-apis")
                .build();
    }

    public void sendTextMessage(String chatId, String text) {
        ensureToken();
        try {
            restClient.post()
                    .uri("/im/v1/messages?receive_id_type=chat_id")
                    .header("Authorization", "Bearer " + tenantAccessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "receive_id", chatId,
                            "msg_type", "text",
                            "content", Map.of("text", text)
                    ))
                    .retrieve();
        } catch (Exception e) {
            log.error("[Feishu] Failed to send message to chat {}: {}", chatId, e.getMessage());
        }
    }

    public void sendCardMessage(String chatId, String cardJson) {
        ensureToken();
        try {
            restClient.post()
                    .uri("/im/v1/messages?receive_id_type=chat_id")
                    .header("Authorization", "Bearer " + tenantAccessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "receive_id", chatId,
                            "msg_type", "interactive",
                            "content", cardJson
                    ))
                    .retrieve();
        } catch (Exception e) {
            log.error("[Feishu] Failed to send card message to chat {}: {}", chatId, e.getMessage());
        }
    }

    private synchronized void ensureToken() {
        if (tenantAccessToken != null && System.currentTimeMillis() < tokenExpiresAt) {
            return;
        }
        try {
            Map<?, ?> resp = restClient.post()
                    .uri("/auth/v3/tenant_access_token/internal")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("app_id", appId, "app_secret", appSecret))
                    .retrieve()
                    .body(Map.class);
            if (resp != null && "0".equals(String.valueOf(resp.get("code")))) {
                tenantAccessToken = (String) resp.get("tenant_access_token");
                Object expireObj = resp.get("expire");
                int expire = expireObj instanceof Number n ? n.intValue() : 7200;
                tokenExpiresAt = System.currentTimeMillis() + (expire - 300) * 1000L;
            }
        } catch (Exception e) {
            log.error("[Feishu] Failed to get tenant_access_token: {}", e.getMessage());
        }
    }
}
