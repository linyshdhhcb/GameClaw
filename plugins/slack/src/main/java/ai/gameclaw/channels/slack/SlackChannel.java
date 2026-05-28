package ai.gameclaw.channels.slack;

import ai.gameclaw.agent.Agent;
import ai.gameclaw.channels.Channel;
import ai.gameclaw.channels.ChannelMessageReceivedEvent;
import ai.gameclaw.channels.ChannelRegistry;
import ai.gameclaw.security.TenantContext;
import ai.gameclaw.security.TenantContextHolder;
import com.slack.api.bolt.App;
import com.slack.api.bolt.context.builtin.SlashCommandContext;
import com.slack.api.bolt.request.builtin.SlashCommandRequest;
import com.slack.api.bolt.response.Response;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.model.event.MessageEvent;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public class SlackChannel implements Channel {

    private static final Logger log = LoggerFactory.getLogger(SlackChannel.class);

    private final ChannelRegistry channelRegistry;
    private final Agent agent;
    private final App slackApp;
    private final SlackTenantRegistry tenantRegistry;
    private final String allowedUser;
    private final AtomicReference<String> lastChannelId = new AtomicReference<>();
    private final AtomicReference<String> lastThreadTs = new AtomicReference<>();
    private final SlackSlashCommandRouter slashCommandRouter;

    public SlackChannel(ChannelRegistry channelRegistry, Agent agent, App slackApp,
                        SlackTenantRegistry tenantRegistry,
                        @org.springframework.beans.factory.annotation.Value("${agent.channels.slack.allowed-user:}") String allowedUser) {
        this.channelRegistry = channelRegistry;
        this.agent = agent;
        this.slackApp = slackApp;
        this.tenantRegistry = tenantRegistry;
        this.allowedUser = allowedUser;
        this.slashCommandRouter = new SlackSlashCommandRouter();
        channelRegistry.registerChannel(this);
    }

    @Override
    public String getName() {
        return "Slack";
    }

    @PostConstruct
    public void registerHandlers() {
        slackApp.command("/gameclaw", (req, ctx) -> {
            String text = req.getPayload().getText();
            String channelId = req.getPayload().getChannelId();
            String userId = req.getPayload().getUserId();
            String teamId = req.getPayload().getTeamId();

            lastChannelId.set(channelId);

            UUID tenantId = tenantRegistry.resolveTenantId(teamId);
            TenantContext tenantCtx = tenantId != null
                    ? TenantContext.of(tenantId)
                    : TenantContextHolder.tryGet().orElse(TenantContext.of(UUID.randomUUID()));

            final String commandText = text != null ? text : "";
            try {
                return TenantContextHolder.runWith(tenantCtx, () -> {
                    SlackSlashCommandRouter.RoutedCommand routed = slashCommandRouter.route(commandText);
                    String conversationId = "slack-" + channelId;

                    channelRegistry.publishMessageReceivedEvent(
                            new ChannelMessageReceivedEvent(getName(), commandText));

                    String response;
                    if ("help".equals(routed.command())) {
                        response = "Available commands: design, query, code, test, help";
                    } else {
                        response = agent.respondTo(conversationId, routed.command() + " " + routed.args());
                    }

                    if (response != null && !response.isBlank()) {
                        ctx.respond(r -> r.responseType("in_channel").text(response));
                    }
                    return ctx.ack();
                });
            } catch (Exception e) {
                log.error("[Slack] Slash command handler error: {}", e.getMessage());
                return ctx.ack();
            }
        });

        slackApp.event(MessageEvent.class, (payload, ctx) -> {
            MessageEvent event = payload.getEvent();

            if (event.getBotId() != null) {
                return ctx.ack();
            }

            if (allowedUser != null && !allowedUser.isBlank()
                    && !allowedUser.equals(event.getUser())) {
                log.warn("[Slack] Ignoring message from unauthorized user: {}", event.getUser());
                return ctx.ack();
            }

            String channelId = event.getChannel();
            String threadTs = event.getThreadTs();
            String teamId = payload.getTeamId();
            String text = event.getText();

            if (text == null || text.isBlank()) {
                return ctx.ack();
            }

            lastChannelId.set(channelId);
            if (threadTs != null) {
                lastThreadTs.set(threadTs);
            }

            UUID tenantId = tenantRegistry.resolveTenantId(teamId);
            TenantContext tenantCtx = tenantId != null
                    ? TenantContext.of(tenantId)
                    : TenantContextHolder.tryGet().orElse(TenantContext.of(UUID.randomUUID()));

            TenantContextHolder.runWith(tenantCtx, () -> {
                String conversationId = buildConversationId(channelId, threadTs);

                channelRegistry.publishMessageReceivedEvent(
                        new ChannelMessageReceivedEvent(getName(), text));

                String response = agent.respondTo(conversationId, text);

                if (response != null && !response.isBlank()) {
                    try {
                        MethodsClient client = slackApp.client();
                        ChatPostMessageRequest.ChatPostMessageRequestBuilder reqBuilder = ChatPostMessageRequest.builder()
                                .channel(channelId)
                                .text(response);
                        if (threadTs != null) {
                            reqBuilder.threadTs(threadTs);
                        }
                        client.chatPostMessage(reqBuilder.build());
                    } catch (Exception e) {
                        log.error("[Slack] Failed to send message: {}", e.getMessage());
                    }
                }
            });

            return ctx.ack();
        });

        log.info("[Slack] Handlers registered for /gameclaw command and MessageEvent");
    }

    @Override
    public void sendMessage(String message) {
        String channelId = lastChannelId.get();
        if (channelId == null || channelId.isBlank()) {
            log.warn("[Slack] No channel available to send message");
            return;
        }
        try {
            MethodsClient client = slackApp.client();
            ChatPostMessageRequest.ChatPostMessageRequestBuilder reqBuilder = ChatPostMessageRequest.builder()
                    .channel(channelId)
                    .text(message);
            String threadTs = lastThreadTs.get();
            if (threadTs != null && !threadTs.isBlank()) {
                reqBuilder.threadTs(threadTs);
            }
            client.chatPostMessage(reqBuilder.build());
        } catch (Exception e) {
            log.error("[Slack] Failed to send message to channel {}: {}", channelId, e.getMessage());
        }
    }

    private String buildConversationId(String channelId, String threadTs) {
        if (threadTs != null && !threadTs.isBlank()) {
            return "slack-" + channelId + "-" + threadTs;
        }
        return "slack-" + channelId;
    }
}
