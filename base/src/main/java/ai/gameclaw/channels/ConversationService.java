package ai.gameclaw.channels;

import ai.gameclaw.security.TenantContext;
import ai.gameclaw.security.TenantContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ConversationService {

    private final ConversationRepository repository;

    public ConversationService(ConversationRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Conversation getOrCreate(String channel) {
        TenantContext ctx = TenantContextHolder.tryGet().orElse(null);
        if (ctx == null || ctx.tenantId() == null || ctx.userId() == null) {
            return null;
        }
        Optional<Conversation> existing = repository.findByChannel(ctx.tenantId(), ctx.userId(), channel);
        if (existing.isPresent()) {
            repository.touchLastActive(existing.get().id());
            return existing.get();
        }
        Conversation conv = Conversation.create(ctx.tenantId(), ctx.userId(), ctx.projectId(), channel, null);
        return repository.save(conv);
    }

    @Transactional
    public Conversation create(UUID tenantId, UUID userId, UUID projectId, String channel, String title) {
        Conversation conv = Conversation.create(tenantId, userId, projectId, channel, title);
        return repository.save(conv);
    }

    @Transactional(readOnly = true)
    public Optional<Conversation> findById(UUID id) {
        return repository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Conversation> findByUserAndProject(UUID tenantId, UUID userId, UUID projectId) {
        return repository.findByUserAndProject(tenantId, userId, projectId);
    }

    @Transactional
    public Conversation updateTitle(UUID id, String title) {
        Conversation conv = repository.findById(id).orElseThrow();
        Conversation updated = new Conversation(
                conv.id(), conv.tenantId(), conv.userId(), conv.projectId(),
                conv.channel(), title, conv.startedAt(), conv.lastActiveAt()
        );
        return repository.save(updated);
    }

    @Transactional
    public void appendMessage(UUID conversationId, UUID tenantId, String role, String content) {
        ConversationMessage msg = ConversationMessage.create(conversationId, tenantId, role, content);
        repository.saveMessage(msg);
    }

    @Transactional(readOnly = true)
    public List<ConversationMessage> loadMessages(UUID conversationId) {
        return repository.findMessagesByConversationId(conversationId);
    }

    @Transactional
    public void deleteConversation(UUID id) {
        repository.deleteMessagesByConversationId(id);
        repository.deleteById(id);
    }
}
