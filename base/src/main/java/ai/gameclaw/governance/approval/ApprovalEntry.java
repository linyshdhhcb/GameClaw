package ai.gameclaw.governance.approval;

import java.time.Instant;
import java.util.UUID;

public record ApprovalEntry(
        UUID userId,
        String decision,
        Instant timestamp,
        String reason
) {}
