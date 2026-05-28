package ai.gameclaw.channels.wecom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.Map;

public class WeComApiClient {

    private static final Logger log = LoggerFactory.getLogger(WeComApiClient.class);

    private final String corpId;
    private final String agentId;
    private final String secret;
    private final RestClient restClient;
    private volatile String accessToken;
    private volatile long expiresAt;

    public WeComApiClient(String corpId, String agentId, String secret) {
        this.corpId = corpId;
        this.agentId = agentId;
        this.secret = secret;
        this.restClient = RestClient.builder()
                .baseUrl("https://qyapi.weixin.qq.com")
                .build();
    }

    public void sendTextMessage(String userId, String text) {
        ensureToken();
        try {
            restClient.post()
                    .uri("/cgi-bin/message/send?access_token=" + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "touser", userId,
                            "msgtype", "text",
                            "agentid", Integer.parseInt(agentId),
                            "text", Map.of("content", text)
                    ))
                    .retrieve();
        } catch (Exception e) {
            log.error("[WeCom] Failed to send text message to user {}: {}", userId, e.getMessage());
        }
    }

    public void sendMarkdownMessage(String userId, String markdown) {
        ensureToken();
        try {
            restClient.post()
                    .uri("/cgi-bin/message/send?access_token=" + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "touser", userId,
                            "msgtype", "markdown",
                            "agentid", Integer.parseInt(agentId),
                            "markdown", Map.of("content", markdown)
                    ))
                    .retrieve();
        } catch (Exception e) {
            log.error("[WeCom] Failed to send markdown message to user {}: {}", userId, e.getMessage());
        }
    }

    private synchronized void ensureToken() {
        if (accessToken != null && System.currentTimeMillis() < expiresAt) {
            return;
        }
        try {
            Map<?, ?> resp = restClient.get()
                    .uri("/cgi-bin/gettoken?corpid=" + corpId + "&corpsecret=" + secret)
                    .retrieve()
                    .body(Map.class);
            if (resp != null && Integer.valueOf(0).equals(resp.get("errcode"))) {
                accessToken = (String) resp.get("access_token");
                Object expireObj = resp.get("expires_in");
                int expire = expireObj instanceof Number n ? n.intValue() : 7200;
                expiresAt = System.currentTimeMillis() + (expire - 300) * 1000L;
            } else {
                log.error("[WeCom] Failed to get access_token: errcode={}, errmsg={}",
                        resp != null ? resp.get("errcode") : null,
                        resp != null ? resp.get("errmsg") : null);
            }
        } catch (Exception e) {
            log.error("[WeCom] Failed to get access_token: {}", e.getMessage());
        }
    }
}
