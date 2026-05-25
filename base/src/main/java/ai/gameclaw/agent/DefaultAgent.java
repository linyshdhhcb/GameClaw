package ai.gameclaw.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.stereotype.Component;

@Component
public class DefaultAgent implements Agent {

    private final ChatClient chatClient;
    private final ChatModel chatModel;

    public DefaultAgent(ChatClient chatClient, ChatModel chatModel) {
        this.chatClient = chatClient;
        this.chatModel = chatModel;
    }

    @Override
    public String respondTo(String conversationId, String question) {
        var promptSpec = chatClient.prompt(question)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId));

        var defaultOptions = chatModel.getDefaultOptions();
        if (defaultOptions instanceof ToolCallingChatOptions toolCallingChatOptions) {
            promptSpec = promptSpec.options(toolCallingChatOptions.mutate());
        }

        return promptSpec.call().content();
    }

    @Override
    public <T> T prompt(String conversationId, String input, Class<T> result) {
        var promptSpec = chatClient.prompt(input)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId));

        var defaultOptions = chatModel.getDefaultOptions();
        if (defaultOptions instanceof ToolCallingChatOptions toolCallingChatOptions) {
            promptSpec = promptSpec.options(toolCallingChatOptions.mutate());
        }

        return promptSpec.call().entity(result);
    }
}
