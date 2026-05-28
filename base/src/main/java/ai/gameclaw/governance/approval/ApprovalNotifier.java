package ai.gameclaw.governance.approval;

import java.util.List;
import java.util.UUID;

public interface ApprovalNotifier {

    void notifyApprovalRequested(PendingApproval approval, List<UUID> approverIds);

    void notifyApprovalCompleted(PendingApproval approval);
}
