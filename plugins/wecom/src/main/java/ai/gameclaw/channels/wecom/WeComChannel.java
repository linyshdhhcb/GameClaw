package ai.gameclaw.channels.wecom;

import ai.gameclaw.agent.Agent;
import ai.gameclaw.channels.Channel;
import ai.gameclaw.channels.ChannelMessageReceivedEvent;
import ai.gameclaw.channels.ChannelRegistry;
import ai.gameclaw.security.TenantContext;
import ai.gameclaw.security.TenantContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public class WeComChannel implements Channel {

    private static final Logger log = LoggerFactory.getLogger(WeComChannel.class);

    private final ChannelRegistry channelRegistry;
    private final Agent agent;
    private final WeComApiClient apiClient;
    private final WeComTenantRegistry tenantRegistry;
    private final AtomicReference<String> lastUserId = new AtomicReference<>();

    public WeComChannel(ChannelRegistry channelRegistry, Agent agent,
                        WeComApiClient apiClient, WeComTenantRegistry tenantRegistry) {
        this.channelRegistry = channelRegistry;
        this.agent = agent;
        this.apiClient = apiClient;
        this.tenantRegistry = tenantRegistry;
        channelRegistry.registerChannel(this);
    }

    @Override
    public String getName() {
        return "WeCom";
    }

    @Override
    public void sendMessage(String message) {
        String userId = lastUserId.get();
        if (userId == null || userId.isEmpty()) {
            log.warn("[WeCom] No user_id available to send message");
            return;
        }
        apiClient.sendTextMessage(userId, message);
    }

    public void handleMessage(String corpId, String userId, String content, String msgType) {
        UUID tenantId = tenantRegistry.resolveTenantId(corpId);
        TenantContext ctx = tenantId != null
                ? TenantContext.of(tenantId)
                : TenantContextHolder.tryGet().orElse(TenantContext.of(UUID.randomUUID()));

        TenantContextHolder.runWith(ctx, () -> {
            lastUserId.set(userId);

            if (content == null || content.isBlank()) return;

            String conversationId = "wecom-" + userId;

            channelRegistry.publishMessageReceivedEvent(
                    new ChannelMessageReceivedEvent(getName(), content));

            String response = agent.respondTo(conversationId, content);

            if (response != null && !response.isBlank()) {
                apiClient.sendTextMessage(userId, response);
            }
        });
    }
}
