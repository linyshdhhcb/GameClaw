package ai.gameclaw.channels.wecom;

import ai.gameclaw.agent.Agent;
import ai.gameclaw.channels.ChannelRegistry;
import ai.gameclaw.security.TenantContext;
import ai.gameclaw.security.TenantContextHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WeComChannelTest {

    private ChannelRegistry channelRegistry;
    private Agent agent;
    private WeComApiClient apiClient;
    private WeComTenantRegistry tenantRegistry;
    private WeComChannel weComChannel;

    @BeforeEach
    void setUp() {
        channelRegistry = new ChannelRegistry();
        agent = mock(Agent.class);
        apiClient = mock(WeComApiClient.class);
        tenantRegistry = mock(WeComTenantRegistry.class);
        weComChannel = new WeComChannel(channelRegistry, agent, apiClient, tenantRegistry);
    }

    @Test
    void getNameReturnsWeCom() {
        assertThat(weComChannel.getName()).isEqualTo("WeCom");
    }

    @Test
    void sendMessageWithoutActiveUserDoesNotThrow() {
        assertThatCode(() -> weComChannel.sendMessage("hello")).doesNotThrowAnyException();
    }

    @Test
    void sendMessageWithActiveUserCallsSendTextMessage() {
        UUID tenantId = UUID.randomUUID();
        TenantContext ctx = TenantContext.of(tenantId);

        TenantContextHolder.runWith(ctx, () -> {
            when(tenantRegistry.resolveTenantId("corp123")).thenReturn(tenantId);
            when(agent.respondTo(anyString(), anyString())).thenReturn("response");

            weComChannel.handleMessage("corp123", "user001", "hello", "text");

            weComChannel.sendMessage("outgoing message");

            verify(apiClient).sendTextMessage("user001", "outgoing message");
        });
    }

    @Test
    void handleMessageCallsAgentRespondTo() {
        UUID tenantId = UUID.randomUUID();
        TenantContext ctx = TenantContext.of(tenantId);

        TenantContextHolder.runWith(ctx, () -> {
            when(tenantRegistry.resolveTenantId("corp123")).thenReturn(tenantId);
            when(agent.respondTo("wecom-user001", "hello")).thenReturn("hi there");

            weComChannel.handleMessage("corp123", "user001", "hello", "text");

            verify(agent).respondTo("wecom-user001", "hello");
        });
    }

    @Test
    void handleMessageRepliesWithAgentResponse() {
        UUID tenantId = UUID.randomUUID();
        TenantContext ctx = TenantContext.of(tenantId);

        TenantContextHolder.runWith(ctx, () -> {
            when(tenantRegistry.resolveTenantId("corp123")).thenReturn(tenantId);
            when(agent.respondTo("wecom-user001", "hello")).thenReturn("hi there");

            weComChannel.handleMessage("corp123", "user001", "hello", "text");

            verify(apiClient).sendTextMessage("user001", "hi there");
        });
    }

    @Test
    void handleMessageDoesNotReplyWhenAgentReturnsBlank() {
        UUID tenantId = UUID.randomUUID();
        TenantContext ctx = TenantContext.of(tenantId);

        TenantContextHolder.runWith(ctx, () -> {
            when(tenantRegistry.resolveTenantId("corp123")).thenReturn(tenantId);
            when(agent.respondTo("wecom-user001", "hello")).thenReturn("");

            weComChannel.handleMessage("corp123", "user001", "hello", "text");

            verify(apiClient, never()).sendTextMessage(anyString(), anyString());
        });
    }

    @Test
    void handleMessageSkipsBlankContent() {
        UUID tenantId = UUID.randomUUID();
        TenantContext ctx = TenantContext.of(tenantId);

        TenantContextHolder.runWith(ctx, () -> {
            when(tenantRegistry.resolveTenantId("corp123")).thenReturn(tenantId);

            weComChannel.handleMessage("corp123", "user001", "", "text");

            verify(agent, never()).respondTo(anyString(), anyString());
            verify(apiClient, never()).sendTextMessage(anyString(), anyString());
        });
    }

    @Test
    void handleMessageSkipsNullContent() {
        UUID tenantId = UUID.randomUUID();
        TenantContext ctx = TenantContext.of(tenantId);

        TenantContextHolder.runWith(ctx, () -> {
            when(tenantRegistry.resolveTenantId("corp123")).thenReturn(tenantId);

            weComChannel.handleMessage("corp123", "user001", null, "text");

            verify(agent, never()).respondTo(anyString(), anyString());
        });
    }

    @Test
    void conversationIdFormatIsWecomPrefixPlusUserId() {
        UUID tenantId = UUID.randomUUID();
        TenantContext ctx = TenantContext.of(tenantId);

        TenantContextHolder.runWith(ctx, () -> {
            when(tenantRegistry.resolveTenantId("corp123")).thenReturn(tenantId);
            when(agent.respondTo(anyString(), anyString())).thenReturn("ok");

            weComChannel.handleMessage("corp123", "userABC", "test", "text");

            verify(agent).respondTo(eq("wecom-userABC"), eq("test"));
        });
    }

    @Test
    void handleMessagePropagatesTenantContext() {
        UUID tenantId = UUID.randomUUID();
        TenantContext ctx = TenantContext.of(tenantId);

        TenantContextHolder.runWith(ctx, () -> {
            when(tenantRegistry.resolveTenantId("corp123")).thenReturn(tenantId);
            when(agent.respondTo(anyString(), anyString())).thenAnswer(invocation -> {
                assertThat(TenantContextHolder.isBound()).isTrue();
                assertThat(TenantContextHolder.require().tenantId()).isEqualTo(tenantId);
                return "ok";
            });

            weComChannel.handleMessage("corp123", "user001", "hello", "text");
        });
    }

    @Test
    void handleMessageFallsBackToRandomTenantWhenUnresolved() {
        when(tenantRegistry.resolveTenantId("unknown_corp")).thenReturn(null);
        when(agent.respondTo(anyString(), anyString())).thenAnswer(invocation -> {
            assertThat(TenantContextHolder.isBound()).isTrue();
            return "ok";
        });

        weComChannel.handleMessage("unknown_corp", "user001", "hello", "text");

        verify(agent).respondTo("wecom-user001", "hello");
    }
}
