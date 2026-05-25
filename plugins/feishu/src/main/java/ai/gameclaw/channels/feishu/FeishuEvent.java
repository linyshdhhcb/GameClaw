package ai.gameclaw.channels.feishu;

public record FeishuEvent(
        String tenantKey,
        String chatId,
        String userId,
        String text,
        String rootId,
        String msgType,
        String messageId
) {
    public boolean isGroupChat() {
        return chatId != null && chatId.startsWith("oc_");
    }

    public boolean isPrivateChat() {
        return chatId != null && chatId.startsWith("ou_");
    }
}
