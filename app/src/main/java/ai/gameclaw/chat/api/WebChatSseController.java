package ai.gameclaw.chat.api;

import ai.gameclaw.agent.Agent;
import ai.gameclaw.channels.Conversation;
import ai.gameclaw.channels.ConversationService;
import ai.gameclaw.security.TenantContext;
import ai.gameclaw.security.TenantContextHolder;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
public class WebChatSseController {

    private final Agent agent;
    private final ConversationService conversationService;

    public WebChatSseController(Agent agent, ConversationService conversationService) {
        this.agent = agent;
        this.conversationService = conversationService;
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> stream(
            @RequestParam String message,
            @RequestParam(defaultValue = "web") String conversationId,
            @RequestParam(required = false) UUID projectId) {

        TenantContext ctx = TenantContextHolder.tryGet().orElse(null);

        String effectiveConvId = conversationId;
        if (ctx != null) {
            Conversation conv = conversationService.getOrCreate("web");
            if (conv != null) {
                effectiveConvId = conv.id().toString();
            }
        }

        final String convId = effectiveConvId;

        Flux<ServerSentEvent<String>> messageFlux = Flux.<ServerSentEvent<String>>create(sink -> {
            Thread.startVirtualThread(() -> {
                try {
                    String response;
                    if (ctx != null) {
                        response = TenantContextHolder.runWith(ctx,
                                (ScopedValue.CallableOp<String, Exception>) () -> agent.respondTo(convId, message));
                    } else {
                        response = agent.respondTo(convId, message);
                    }
                    sink.next(ServerSentEvent.<String>builder().event("message").data(response).build());
                    sink.next(ServerSentEvent.<String>builder().event("done").data("[DONE]").build());
                    sink.complete();
                } catch (Exception e) {
                    sink.next(ServerSentEvent.<String>builder().event("error").data(e.getMessage()).build());
                    sink.complete();
                }
            });
        });

        Flux<ServerSentEvent<String>> heartbeat = Flux.interval(Duration.ofSeconds(15))
                .map(tick -> ServerSentEvent.<String>builder().event("heartbeat").data("").build());

        return Flux.merge(messageFlux, heartbeat)
                .takeUntil(sse -> "done".equals(sse.event()) || "error".equals(sse.event()));
    }
}
