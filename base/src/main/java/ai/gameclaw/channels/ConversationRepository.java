package ai.gameclaw.channels;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ConversationRepository {

    private static final RowMapper<Conversation> CONV_ROW_MAPPER = (rs, rowNum) -> new Conversation(
            rs.getObject("id", UUID.class),
            rs.getObject("tenant_id", UUID.class),
            rs.getObject("user_id", UUID.class),
            rs.getObject("project_id", UUID.class),
            rs.getString("channel"),
            rs.getString("title"),
            rs.getTimestamp("started_at").toInstant(),
            rs.getTimestamp("last_active_at").toInstant()
    );

    private static final RowMapper<ConversationMessage> MSG_ROW_MAPPER = (rs, rowNum) -> new ConversationMessage(
            rs.getObject("id", UUID.class),
            rs.getObject("conversation_id", UUID.class),
            rs.getObject("tenant_id", UUID.class),
            rs.getString("role"),
            rs.getString("content"),
            rs.getTimestamp("created_at").toInstant()
    );

    private final JdbcTemplate jdbc;

    public ConversationRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public Conversation save(Conversation conv) {
        if (conv.id() == null) {
            return insert(conv);
        }
        return update(conv);
    }

    private Conversation insert(Conversation conv) {
        jdbc.update(
                "INSERT INTO conversations (tenant_id, user_id, project_id, channel, title, started_at, last_active_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)",
                conv.tenantId(), conv.userId(), conv.projectId(), conv.channel(), conv.title(),
                conv.startedAt(), conv.lastActiveAt()
        );
        return jdbc.queryForObject(
                "SELECT * FROM conversations WHERE tenant_id = ? AND user_id = ? AND channel = ? ORDER BY started_at DESC LIMIT 1",
                CONV_ROW_MAPPER, conv.tenantId(), conv.userId(), conv.channel()
        );
    }

    private Conversation update(Conversation conv) {
        jdbc.update(
                "UPDATE conversations SET title = ?, last_active_at = ? WHERE id = ?",
                conv.title(), conv.lastActiveAt(), conv.id()
        );
        return findById(conv.id()).orElseThrow();
    }

    @Transactional(readOnly = true)
    public Optional<Conversation> findById(UUID id) {
        List<Conversation> results = jdbc.query(
                "SELECT * FROM conversations WHERE id = ?", CONV_ROW_MAPPER, id
        );
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Transactional(readOnly = true)
    public List<Conversation> findByUserAndProject(UUID tenantId, UUID userId, UUID projectId) {
        if (projectId != null) {
            return jdbc.query(
                    "SELECT * FROM conversations WHERE tenant_id = ? AND user_id = ? AND project_id = ? ORDER BY last_active_at DESC",
                    CONV_ROW_MAPPER, tenantId, userId, projectId
            );
        }
        return jdbc.query(
                "SELECT * FROM conversations WHERE tenant_id = ? AND user_id = ? AND project_id IS NULL ORDER BY last_active_at DESC",
                CONV_ROW_MAPPER, tenantId, userId
        );
    }

    @Transactional(readOnly = true)
    public Optional<Conversation> findByChannel(UUID tenantId, UUID userId, String channel) {
        List<Conversation> results = jdbc.query(
                "SELECT * FROM conversations WHERE tenant_id = ? AND user_id = ? AND channel = ? ORDER BY last_active_at DESC LIMIT 1",
                CONV_ROW_MAPPER, tenantId, userId, channel
        );
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Transactional
    public void touchLastActive(UUID id) {
        jdbc.update("UPDATE conversations SET last_active_at = now() WHERE id = ?", id);
    }

    @Transactional
    public void deleteById(UUID id) {
        jdbc.update("DELETE FROM conversations WHERE id = ?", id);
    }

    @Transactional
    public ConversationMessage saveMessage(ConversationMessage msg) {
        jdbc.update(
                "INSERT INTO conversation_messages (conversation_id, tenant_id, role, content, created_at) VALUES (?, ?, ?, ?::jsonb, ?)",
                msg.conversationId(), msg.tenantId(), msg.role(), msg.content(), msg.createdAt()
        );
        return jdbc.queryForObject(
                "SELECT * FROM conversation_messages WHERE conversation_id = ? ORDER BY created_at DESC LIMIT 1",
                MSG_ROW_MAPPER, msg.conversationId()
        );
    }

    @Transactional(readOnly = true)
    public List<ConversationMessage> findMessagesByConversationId(UUID conversationId) {
        return jdbc.query(
                "SELECT * FROM conversation_messages WHERE conversation_id = ? ORDER BY created_at ASC",
                MSG_ROW_MAPPER, conversationId
        );
    }

    @Transactional
    public void deleteMessagesByConversationId(UUID conversationId) {
        jdbc.update("DELETE FROM conversation_messages WHERE conversation_id = ?", conversationId);
    }
}
