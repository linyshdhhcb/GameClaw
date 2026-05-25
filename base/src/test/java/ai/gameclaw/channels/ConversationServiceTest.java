package ai.gameclaw.channels;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

    @Mock
    private ConversationRepository repository;

    private ConversationService service;

    private UUID tenantId;
    private UUID userId;
    private UUID projectId;

    @BeforeEach
    void setUp() {
        service = new ConversationService(repository);
        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();
        projectId = UUID.randomUUID();
    }

    @Test
    void createConversation() {
        Conversation conv = Conversation.create(tenantId, userId, projectId, "web", "Test Chat");
        when(repository.save(any(Conversation.class))).thenReturn(conv);

        Conversation result = service.create(tenantId, userId, projectId, "web", "Test Chat");

        assertThat(result).isNotNull();
        assertThat(result.tenantId()).isEqualTo(tenantId);
        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.channel()).isEqualTo("web");
        verify(repository).save(any(Conversation.class));
    }

    @Test
    void appendMessage() {
        UUID convId = UUID.randomUUID();
        when(repository.saveMessage(any(ConversationMessage.class))).thenAnswer(inv -> inv.getArgument(0));

        service.appendMessage(convId, tenantId, "user", "{\"text\":\"hello\"}");

        verify(repository).saveMessage(any(ConversationMessage.class));
    }

    @Test
    void loadMessages() {
        UUID convId = UUID.randomUUID();
        ConversationMessage msg = ConversationMessage.create(convId, tenantId, "user", "{\"text\":\"hello\"}");
        when(repository.findMessagesByConversationId(convId)).thenReturn(List.of(msg));

        List<ConversationMessage> messages = service.loadMessages(convId);

        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).role()).isEqualTo("user");
    }

    @Test
    void deleteConversation() {
        UUID convId = UUID.randomUUID();

        service.deleteConversation(convId);

        verify(repository).deleteMessagesByConversationId(convId);
        verify(repository).deleteById(convId);
    }

    @Test
    void findByUserAndProject() {
        when(repository.findByUserAndProject(tenantId, userId, projectId)).thenReturn(List.of());

        List<Conversation> result = service.findByUserAndProject(tenantId, userId, projectId);

        assertThat(result).isEmpty();
        verify(repository).findByUserAndProject(tenantId, userId, projectId);
    }
}
