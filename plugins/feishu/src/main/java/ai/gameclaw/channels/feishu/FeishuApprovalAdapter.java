package ai.gameclaw.channels.feishu;

import ai.gameclaw.governance.approval.ApprovalNotifier;
import ai.gameclaw.governance.approval.ApprovalState;
import ai.gameclaw.governance.approval.PendingApproval;
import ai.gameclaw.security.RiskLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "agent.channels.feishu", name = {"app-id", "app-secret"})
public class FeishuApprovalAdapter implements ApprovalNotifier {

    private static final Logger log = LoggerFactory.getLogger(FeishuApprovalAdapter.class);

    private final FeishuApiClient apiClient;
    private final String defaultChatId;

    public FeishuApprovalAdapter(FeishuApiClient apiClient,
                                 @Value("${agent.channels.feishu.default-chat-id:}") String defaultChatId) {
        this.apiClient = apiClient;
        this.defaultChatId = defaultChatId;
    }

    @Override
    public void notifyApprovalRequested(PendingApproval approval, List<UUID> approverIds) {
        if (defaultChatId == null || defaultChatId.isBlank()) {
            log.warn("[FeishuApprovalAdapter] No default chatId configured, skipping notification");
            return;
        }

        String template = approval.riskLevel() == RiskLevel.L5_PRODUCTION ? "red" : "blue";

        String impactDesc = approval.impactSummary() != null ? approval.impactSummary() : "N/A";

        String cardJson = FeishuCardBuilder.create()
                .header("GameClaw 审批请求", template)
                .markdown("**资源**: " + approval.resource()
                        + "\n**操作**: " + approval.action()
                        + "\n**风险等级**: " + approval.riskLevel().name()
                        + "\n**影响域**: " + impactDesc + " 个文件"
                        + "\n**Quorum**: " + approval.quorum())
                .divider()
                .action("通过", "/api/approval/" + approval.id() + "/approve", "primary")
                .action("拒绝", "/api/approval/" + approval.id() + "/reject", "danger")
                .build();

        apiClient.sendCardMessage(defaultChatId, cardJson);
        log.info("[FeishuApprovalAdapter] Sent approval notification for {} to chat {}",
                approval.id(), defaultChatId);
    }

    @Override
    public void notifyApprovalCompleted(PendingApproval approval) {
        if (defaultChatId == null || defaultChatId.isBlank()) {
            log.warn("[FeishuApprovalAdapter] No default chatId configured, skipping notification");
            return;
        }

        String statusText = approval.state() == ApprovalState.APPROVED ? "已通过" : "已拒绝";
        String text = "审批 " + approval.id() + " " + statusText
                + "\n资源: " + approval.resource()
                + "\n操作: " + approval.action();

        apiClient.sendTextMessage(defaultChatId, text);
        log.info("[FeishuApprovalAdapter] Sent completion notification for {} to chat {}",
                approval.id(), defaultChatId);
    }
}
