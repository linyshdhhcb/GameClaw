package ai.gameclaw.channels;

import java.time.Instant;
import java.util.UUID;

public record ConversationMessage(
        UUID id,
        UUID conversationId,
        UUID tenantId,
        String role,
        String content,
        Instant createdAt
) {

    public static ConversationMessage create(UUID conversationId, UUID tenantId, String role, String content) {
        return new ConversationMessage(null, conversationId, tenantId, role, content, Instant.now());
    }
}
