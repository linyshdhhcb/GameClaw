package ai.gameclaw.governance.approval;

import java.util.UUID;

public class ApprovalRequiredException extends RuntimeException {

    private final UUID approvalId;

    public ApprovalRequiredException(UUID approvalId) {
        super("Approval required: " + approvalId);
        this.approvalId = approvalId;
    }

    public UUID getApprovalId() {
        return approvalId;
    }
}
