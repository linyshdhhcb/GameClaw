package ai.gameclaw.governance.approval;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class CompositeApprovalNotifier implements ApprovalNotifier {

    private final ObjectProvider<ApprovalNotifier> notifiersProvider;

    public CompositeApprovalNotifier(ObjectProvider<ApprovalNotifier> notifiersProvider) {
        this.notifiersProvider = notifiersProvider;
    }

    @Override
    public void notifyApprovalRequested(PendingApproval approval, List<UUID> approverIds) {
        for (ApprovalNotifier notifier : getDelegates()) {
            notifier.notifyApprovalRequested(approval, approverIds);
        }
    }

    @Override
    public void notifyApprovalCompleted(PendingApproval approval) {
        for (ApprovalNotifier notifier : getDelegates()) {
            notifier.notifyApprovalCompleted(approval);
        }
    }

    private List<ApprovalNotifier> getDelegates() {
        return notifiersProvider.orderedStream()
                .filter(n -> n != this)
                .toList();
    }
}
