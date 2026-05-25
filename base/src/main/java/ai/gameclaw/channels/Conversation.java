package ai.gameclaw.channels;

import java.time.Instant;
import java.util.UUID;

public record Conversation(
        UUID id,
        UUID tenantId,
        UUID userId,
        UUID projectId,
        String channel,
        String title,
        Instant startedAt,
        Instant lastActiveAt
) {

    public static Conversation create(UUID tenantId, UUID userId, UUID projectId, String channel, String title) {
        Instant now = Instant.now();
        return new Conversation(null, tenantId, userId, projectId, channel, title, now, now);
    }
}
