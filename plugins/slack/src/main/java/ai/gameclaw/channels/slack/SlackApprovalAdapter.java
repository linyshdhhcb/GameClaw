package ai.gameclaw.channels.slack;

import ai.gameclaw.governance.approval.ApprovalNotifier;
import ai.gameclaw.governance.approval.ApprovalState;
import ai.gameclaw.governance.approval.PendingApproval;
import com.slack.api.bolt.App;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "agent.channels.slack", name = {"token", "app-token"})
public class SlackApprovalAdapter implements ApprovalNotifier {

    private static final Logger log = LoggerFactory.getLogger(SlackApprovalAdapter.class);

    private final App slackApp;
    private final String defaultChannelId;

    public SlackApprovalAdapter(App slackApp,
                                @Value("${agent.channels.slack.default-channel-id:}") String defaultChannelId) {
        this.slackApp = slackApp;
        this.defaultChannelId = defaultChannelId;
    }

    @Override
    public void notifyApprovalRequested(PendingApproval approval, List<UUID> approverIds) {
        if (defaultChannelId == null || defaultChannelId.isBlank()) {
            log.warn("[SlackApprovalAdapter] No default channel configured, skipping notification");
            return;
        }

        try {
            MethodsClient client = slackApp.client();

            String impactDesc = approval.impactSummary() != null ? approval.impactSummary() : "N/A";

            String blocks = buildApprovalBlocks(approval, impactDesc);

            ChatPostMessageRequest request = ChatPostMessageRequest.builder()
                    .channel(defaultChannelId)
                    .text("GameClaw 审批请求: " + approval.action())
                    .blocksAsString(blocks)
                    .build();

            client.chatPostMessage(request);
            log.info("[SlackApprovalAdapter] Sent approval notification for {} to channel {}",
                    approval.id(), defaultChannelId);
        } catch (Exception e) {
            log.error("[SlackApprovalAdapter] Failed to send approval notification: {}", e.getMessage());
        }
    }

    @Override
    public void notifyApprovalCompleted(PendingApproval approval) {
        if (defaultChannelId == null || defaultChannelId.isBlank()) {
            log.warn("[SlackApprovalAdapter] No default channel configured, skipping notification");
            return;
        }

        try {
            MethodsClient client = slackApp.client();

            String statusText = approval.state() == ApprovalState.APPROVED ? "已通过" : "已拒绝";
            String text = "审批 " + approval.id() + " " + statusText
                    + " | 资源: " + approval.resource()
                    + " | 操作: " + approval.action();

            ChatPostMessageRequest request = ChatPostMessageRequest.builder()
                    .channel(defaultChannelId)
                    .text(text)
                    .build();

            client.chatPostMessage(request);
            log.info("[SlackApprovalAdapter] Sent completion notification for {} to channel {}",
                    approval.id(), defaultChannelId);
        } catch (Exception e) {
            log.error("[SlackApprovalAdapter] Failed to send completion notification: {}", e.getMessage());
        }
    }

    private String buildApprovalBlocks(PendingApproval approval, String impactDesc) {
        return "["
                + "{\"type\":\"header\",\"text\":{\"type\":\"plain_text\",\"text\":\"GameClaw 审批请求\"}},"
                + "{\"type\":\"section\",\"fields\":["
                + "{\"type\":\"mrkdwn\",\"text\":\"*资源:* " + approval.resource() + "\"},"
                + "{\"type\":\"mrkdwn\",\"text\":\"*操作:* " + approval.action() + "\"},"
                + "{\"type\":\"mrkdwn\",\"text\":\"*风险等级:* " + approval.riskLevel().name() + "\"},"
                + "{\"type\":\"mrkdwn\",\"text\":\"*影响域:* " + impactDesc + " 个文件\"}"
                + "]},"
                + "{\"type\":\"actions\",\"elements\":["
                + "{\"type\":\"button\",\"text\":{\"type\":\"plain_text\",\"text\":\"通过\"},\"url\":\"/api/approval/" + approval.id() + "/approve\",\"style\":\"primary\"},"
                + "{\"type\":\"button\",\"text\":{\"type\":\"plain_text\",\"text\":\"拒绝\"},\"url\":\"/api/approval/" + approval.id() + "/reject\",\"style\":\"danger\"}"
                + "]}"
                + "]";
    }
}
