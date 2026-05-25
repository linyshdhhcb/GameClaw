package ai.gameclaw.channels;

import ai.gameclaw.security.TenantContext;
import ai.gameclaw.security.TenantContextHolder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.memory.AppendableChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@Primary
@ConditionalOnBean(ConversationRepository.class)
@ConditionalOnProperty(name = "gameclaw.conversations.storage", havingValue = "jdbc", matchIfMissing = true)
public class JdbcChatMemoryRepository implements AppendableChatMemoryRepository {

    private final ConversationRepository conversationRepository;
    private final ConversationService conversationService;
    private final ObjectMapper objectMapper;

    public JdbcChatMemoryRepository(ConversationRepository conversationRepository,
                                    ConversationService conversationService) {
        this.conversationRepository = conversationRepository;
        this.conversationService = conversationService;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findConversationIds() {
        TenantContext ctx = TenantContextHolder.tryGet().orElse(null);
        if (ctx == null || ctx.tenantId() == null || ctx.userId() == null) {
            return List.of();
        }
        List<Conversation> convs = conversationRepository.findByUserAndProject(
                ctx.tenantId(), ctx.userId(), ctx.projectId());
        List<String> ids = new ArrayList<>();
        for (Conversation conv : convs) {
            ids.add(conv.id().toString());
        }
        return ids;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Message> findByConversationId(String conversationId) {
        UUID convUuid = tryParseUuid(conversationId);
        if (convUuid == null) {
            return List.of();
        }
        List<ConversationMessage> messages = conversationRepository.findMessagesByConversationId(convUuid);
        List<Message> result = new ArrayList<>();
        for (ConversationMessage msg : messages) {
            result.add(toSpringAiMessage(msg));
        }
        return result;
    }

    @Override
    @Transactional
    public void appendAll(String conversationId, List<Message> messages) {
        UUID convUuid = resolveConversationUuid(conversationId);
        if (convUuid == null) return;
        TenantContext ctx = TenantContextHolder.tryGet().orElse(null);
        UUID tenantId = ctx != null ? ctx.tenantId() : null;
        for (Message msg : messages) {
            String content = serializeContent(msg);
            conversationService.appendMessage(convUuid, tenantId, messageType(msg), content);
        }
    }

    @Override
    @Transactional
    public void saveAll(String conversationId, List<Message> messages) {
        UUID convUuid = resolveConversationUuid(conversationId);
        if (convUuid == null) return;
        TenantContext ctx = TenantContextHolder.tryGet().orElse(null);
        UUID tenantId = ctx != null ? ctx.tenantId() : null;
        conversationRepository.deleteMessagesByConversationId(convUuid);
        for (Message msg : messages) {
            String content = serializeContent(msg);
            conversationService.appendMessage(convUuid, tenantId, messageType(msg), content);
        }
    }

    @Override
    @Transactional
    public void deleteByConversationId(String conversationId) {
        UUID convUuid = tryParseUuid(conversationId);
        if (convUuid == null) return;
        conversationService.deleteConversation(convUuid);
    }

    private UUID tryParseUuid(String conversationId) {
        try {
            return UUID.fromString(conversationId);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private UUID resolveConversationUuid(String conversationId) {
        UUID uuid = tryParseUuid(conversationId);
        if (uuid != null) return uuid;
        Conversation conv = conversationService.getOrCreate(conversationId);
        return conv != null ? conv.id() : null;
    }

    private Message toSpringAiMessage(ConversationMessage msg) {
        String content = deserializeContent(msg.content());
        return switch (msg.role()) {
            case "user" -> new UserMessage(content);
            case "assistant" -> new AssistantMessage(content);
            case "system" -> new SystemMessage(content);
            default -> new UserMessage(content);
        };
    }

    private String messageType(Message msg) {
        if (msg instanceof UserMessage) return "user";
        if (msg instanceof AssistantMessage) return "assistant";
        if (msg instanceof SystemMessage) return "system";
        return "user";
    }

    private String serializeContent(Message msg) {
        try {
            return objectMapper.writeValueAsString(Map.of("text", msg.getText()));
        } catch (JsonProcessingException e) {
            return "{\"text\":\"\"}";
        }
    }

    private String deserializeContent(String jsonb) {
        try {
            Map<?, ?> map = objectMapper.readValue(jsonb, Map.class);
            Object text = map.get("text");
            return text != null ? text.toString() : jsonb;
        } catch (JsonProcessingException e) {
            return jsonb;
        }
    }
}
