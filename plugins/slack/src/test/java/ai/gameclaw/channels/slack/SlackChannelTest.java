package ai.gameclaw.channels.slack;

import ai.gameclaw.agent.Agent;
import ai.gameclaw.channels.ChannelRegistry;
import com.slack.api.app_backend.events.payload.EventsApiPayload;
import com.slack.api.bolt.App;
import com.slack.api.bolt.context.builtin.EventContext;
import com.slack.api.bolt.handler.BoltEventHandler;
import com.slack.api.bolt.response.Response;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.model.event.MessageEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SlackChannelTest {

    private ChannelRegistry channelRegistry;
    private Agent agent;
    private App slackApp;
    private SlackTenantRegistry tenantRegistry;
    private SlackChannel slackChannel;

    @BeforeEach
    void setUp() {
        channelRegistry = new ChannelRegistry();
        agent = mock(Agent.class);
        slackApp = mock(App.class);
        tenantRegistry = mock(SlackTenantRegistry.class);
        slackChannel = new SlackChannel(channelRegistry, agent, slackApp, tenantRegistry, "");
    }

    @Test
    void getNameReturnsSlack() {
        assertThat(slackChannel.getName()).isEqualTo("Slack");
    }

    @Test
    void sendMessageWithoutActiveChannelDoesNotThrow() {
        assertThatCode(() -> slackChannel.sendMessage("hello")).doesNotThrowAnyException();
    }

    @Test
    void sendMessageWithActiveChannelCallsChatPostMessage() throws Exception {
        MethodsClient methodsClient = mock(MethodsClient.class);
        when(slackApp.client()).thenReturn(methodsClient);
        setLastChannelId(slackChannel, "C123456");

        slackChannel.sendMessage("hello world");

        verify(methodsClient).chatPostMessage(any(ChatPostMessageRequest.class));
    }

    @Test
    void sendMessageWithThreadTsIncludesThreadTs() throws Exception {
        MethodsClient methodsClient = mock(MethodsClient.class);
        when(slackApp.client()).thenReturn(methodsClient);
        setLastChannelId(slackChannel, "C123456");
        setLastThreadTs(slackChannel, "1234567890.123456");

        slackChannel.sendMessage("threaded reply");

        verify(methodsClient).chatPostMessage(any(ChatPostMessageRequest.class));
    }

    @Test
    void buildConversationIdWithThreadTs() throws Exception {
        String result = invokeBuildConversationId(slackChannel, "C123", "1234567890.123456");
        assertThat(result).isEqualTo("slack-C123-1234567890.123456");
    }

    @Test
    void buildConversationIdWithoutThreadTs() throws Exception {
        String result = invokeBuildConversationId(slackChannel, "C123", null);
        assertThat(result).isEqualTo("slack-C123");
    }

    @Test
    void buildConversationIdWithBlankThreadTs() throws Exception {
        String result = invokeBuildConversationId(slackChannel, "C123", "");
        assertThat(result).isEqualTo("slack-C123");
    }

    @Test
    void botMessagesAreIgnored() throws Exception {
        slackChannel.registerHandlers();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<BoltEventHandler<MessageEvent>> captor = ArgumentCaptor.forClass(BoltEventHandler.class);
        verify(slackApp).event(eq(MessageEvent.class), captor.capture());

        BoltEventHandler<MessageEvent> handler = captor.getValue();

        @SuppressWarnings("unchecked")
        EventsApiPayload<MessageEvent> payload = mock(EventsApiPayload.class);
        EventContext ctx = mock(EventContext.class);
        MessageEvent event = mock(MessageEvent.class);

        when(payload.getEvent()).thenReturn(event);
        when(event.getBotId()).thenReturn("B123456");
        when(ctx.ack()).thenReturn(new Response());

        Response response = handler.apply(payload, ctx);

        assertThat(response).isNotNull();
        verify(agent, never()).respondTo(anyString(), anyString());
    }

    @Test
    void unauthorizedUserMessagesAreIgnored() throws Exception {
        SlackChannel filteredChannel = new SlackChannel(
                new ChannelRegistry(), agent, slackApp, tenantRegistry, "U_AUTHORIZED");
        filteredChannel.registerHandlers();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<BoltEventHandler<MessageEvent>> captor = ArgumentCaptor.forClass(BoltEventHandler.class);
        verify(slackApp).event(eq(MessageEvent.class), captor.capture());

        BoltEventHandler<MessageEvent> handler = captor.getValue();

        @SuppressWarnings("unchecked")
        EventsApiPayload<MessageEvent> payload = mock(EventsApiPayload.class);
        EventContext ctx = mock(EventContext.class);
        MessageEvent event = mock(MessageEvent.class);

        when(payload.getEvent()).thenReturn(event);
        when(payload.getTeamId()).thenReturn("T123");
        when(event.getBotId()).thenReturn(null);
        when(event.getUser()).thenReturn("U_UNAUTHORIZED");
        when(event.getChannel()).thenReturn("C123");
        when(event.getText()).thenReturn("hello");
        when(tenantRegistry.resolveTenantId("T123")).thenReturn(null);
        when(ctx.ack()).thenReturn(new Response());

        Response response = handler.apply(payload, ctx);

        assertThat(response).isNotNull();
        verify(agent, never()).respondTo(anyString(), anyString());
    }

    @Test
    void authorizedUserMessageIsProcessed() throws Exception {
        SlackChannel filteredChannel = new SlackChannel(
                new ChannelRegistry(), agent, slackApp, tenantRegistry, "U_AUTHORIZED");
        filteredChannel.registerHandlers();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<BoltEventHandler<MessageEvent>> captor = ArgumentCaptor.forClass(BoltEventHandler.class);
        verify(slackApp).event(eq(MessageEvent.class), captor.capture());

        BoltEventHandler<MessageEvent> handler = captor.getValue();

        @SuppressWarnings("unchecked")
        EventsApiPayload<MessageEvent> payload = mock(EventsApiPayload.class);
        EventContext ctx = mock(EventContext.class);
        MessageEvent event = mock(MessageEvent.class);
        MethodsClient methodsClient = mock(MethodsClient.class);

        when(payload.getEvent()).thenReturn(event);
        when(payload.getTeamId()).thenReturn("T123");
        when(event.getBotId()).thenReturn(null);
        when(event.getUser()).thenReturn("U_AUTHORIZED");
        when(event.getChannel()).thenReturn("C123");
        when(event.getText()).thenReturn("hello");
        when(tenantRegistry.resolveTenantId("T123")).thenReturn(null);
        when(slackApp.client()).thenReturn(methodsClient);
        when(agent.respondTo(anyString(), eq("hello"))).thenReturn("response");
        when(ctx.ack()).thenReturn(new Response());

        Response response = handler.apply(payload, ctx);

        assertThat(response).isNotNull();
        verify(agent).respondTo(eq("slack-C123"), eq("hello"));
    }

    private void setLastChannelId(SlackChannel channel, String channelId) throws Exception {
        Field field = SlackChannel.class.getDeclaredField("lastChannelId");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        AtomicReference<String> ref = (AtomicReference<String>) field.get(channel);
        ref.set(channelId);
    }

    private void setLastThreadTs(SlackChannel channel, String threadTs) throws Exception {
        Field field = SlackChannel.class.getDeclaredField("lastThreadTs");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        AtomicReference<String> ref = (AtomicReference<String>) field.get(channel);
        ref.set(threadTs);
    }

    private String invokeBuildConversationId(SlackChannel channel, String channelId, String threadTs) throws Exception {
        Method method = SlackChannel.class.getDeclaredMethod("buildConversationId", String.class, String.class);
        method.setAccessible(true);
        return (String) method.invoke(channel, channelId, threadTs);
    }
}
