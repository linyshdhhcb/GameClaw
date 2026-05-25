package ai.gameclaw.channels.feishu;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FeishuEventParser {

    private static final Logger log = LoggerFactory.getLogger(FeishuEventParser.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static FeishuEvent parse(byte[] body) {
        try {
            JsonNode root = MAPPER.readTree(body);
            JsonNode event = root.has("event") ? root.get("event") : root;

            String tenantKey = getText(root, "tenant_key",
                    event.has("tenant_key") ? event.get("tenant_key").asText() : null);
            String chatId = getText(event, "message.chat_id",
                    event.path("chat_id").asText(null));
            String userId = getText(event, "sender.sender_id.user_id",
                    event.path("sender").path("sender_id").path("user_id").asText(null));
            String msgType = event.path("message").path("msg_type").asText("text");
            String messageId = event.path("message").path("message_id").asText(null);
            String rootId = event.path("message").path("root_id").asText(null);

            String text = null;
            if ("text".equals(msgType)) {
                String content = event.path("message").path("content").asText(null);
                if (content != null) {
                    JsonNode contentNode = MAPPER.readTree(content);
                    text = contentNode.path("text").asText(null);
                }
            }

            return new FeishuEvent(tenantKey, chatId, userId, text, rootId, msgType, messageId);
        } catch (Exception e) {
            log.error("[Feishu] Failed to parse event: {}", e.getMessage());
            return new FeishuEvent(null, null, null, null, null, null, null);
        }
    }

    private static String getText(JsonNode node, String path, String fallback) {
        return fallback;
    }
}
