package ai.gameclaw.agent.llm;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
@ConditionalOnProperty(name = "gameclaw.llm.adapter", havingValue = "spring-ai", matchIfMissing = true)
public class SpringAiLlmClient implements LlmClient {

    private final ChatClient chatClient;

    public SpringAiLlmClient(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public ChatResponse call(ChatRequest request) {
        var response = chatClient.prompt(request.prompt()).call();
        var content = response.content();
        return new ChatResponse(content != null ? content : "");
    }

    @Override
    public Flux<ChatResponseChunk> stream(ChatRequest request) {
        return chatClient.prompt(request.prompt())
                .stream()
                .chatResponse()
                .map(sr -> {
                    var delta = sr.getResult();
                    var text = delta != null && delta.getOutput() != null ? delta.getOutput().getText() : "";
                    return new ChatResponseChunk(text != null ? text : "", false);
                })
                .concatWith(Flux.just(new ChatResponseChunk("", true)));
    }

    @Override
    public EmbeddingResponse embed(EmbeddingRequest request) {
        throw new UnsupportedOperationException("Embedding via Spring AI is not yet implemented in this adapter");
    }
}
