package ai.gameclaw.channels.feishu;

import ai.gameclaw.agent.Agent;
import ai.gameclaw.channels.Channel;
import ai.gameclaw.channels.ChannelMessageReceivedEvent;
import ai.gameclaw.channels.ChannelRegistry;
import ai.gameclaw.security.TenantContext;
import ai.gameclaw.security.TenantContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FeishuChannel implements Channel {

    private static final Logger log = LoggerFactory.getLogger(FeishuChannel.class);

    private final ChannelRegistry channelRegistry;
    private final Agent agent;
    private final FeishuApiClient apiClient;
    private final FeishuTenantRegistry tenantRegistry;
    private final SlashCommandRouter slashCommandRouter;
    private final String lastChatId = new String();
    private final Map<String, String> threadKeys = new ConcurrentHashMap<>();

    public FeishuChannel(ChannelRegistry channelRegistry, Agent agent,
                         FeishuApiClient apiClient, FeishuTenantRegistry tenantRegistry,
                         SlashCommandRouter slashCommandRouter) {
        this.channelRegistry = channelRegistry;
        this.agent = agent;
        this.apiClient = apiClient;
        this.tenantRegistry = tenantRegistry;
        this.slashCommandRouter = slashCommandRouter;
        channelRegistry.registerChannel(this);
    }

    @Override
    public String getName() {
        return "Feishu";
    }

    @Override
    public void sendMessage(String message) {
        if (lastChatId.isEmpty()) {
            log.warn("[Feishu] No chat_id available to send message");
            return;
        }
        apiClient.sendTextMessage(lastChatId, message);
    }

    public void handleEvent(FeishuEvent event) {
        UUID tenantId = tenantRegistry.resolveTenantId(event.tenantKey());
        TenantContext ctx = tenantId != null
                ? TenantContext.of(tenantId)
                : TenantContextHolder.tryGet().orElse(TenantContext.of(UUID.randomUUID()));

        TenantContextHolder.runWith(ctx, () -> {
            String chatId = event.chatId();
            String userId = event.userId();
            String text = event.text();

            if (text == null || text.isBlank()) return;

            String conversationId = "feishu-" + chatId;
            if (event.rootId() != null && !event.rootId().isEmpty()) {
                conversationId = "feishu-" + chatId + "-" + event.rootId();
            }

            channelRegistry.publishMessageReceivedEvent(
                    new ChannelMessageReceivedEvent(getName(), text));

            String response;
            if (text.startsWith("/")) {
                response = slashCommandRouter.route(text, event);
            } else {
                response = agent.respondTo(conversationId, text);
            }

            if (response != null && !response.isBlank()) {
                String cardJson = FeishuCardBuilder.wrapResponse(response);
                if (cardJson != null) {
                    apiClient.sendCardMessage(chatId, cardJson);
                } else {
                    apiClient.sendTextMessage(chatId, response);
                }
            }
        });
    }
}
