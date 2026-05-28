package ai.gameclaw.governance.rollback;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record RollbackSnapshot(
        UUID id,
        UUID tenantId,
        UUID approvalId,
        String resource,
        RollbackKind kind,
        Map<String, Object> snapshotData,
        Instant createdAt
) {}
