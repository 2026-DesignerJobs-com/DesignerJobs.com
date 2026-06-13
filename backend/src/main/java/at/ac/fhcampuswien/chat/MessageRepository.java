package at.ac.fhcampuswien.chat;

import at.ac.fhcampuswien.infrastructure.Database.Database;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
public class MessageRepository {

    public MessageRepository() {
        createTableIfNotExists();
    }

    private void createTableIfNotExists() {
        String sql = """
            CREATE TABLE IF NOT EXISTS messages (
                id VARCHAR(255) PRIMARY KEY,
                conversation_id VARCHAR(255) NOT NULL,
                sender_id VARCHAR(255) NOT NULL,
                content TEXT NOT NULL,
                flagged BOOLEAN NOT NULL DEFAULT FALSE,
                created_at VARCHAR(255) NOT NULL
            )
        """;

        String indexSql = """
            CREATE INDEX IF NOT EXISTS idx_messages_conversation_created
            ON messages (conversation_id, created_at DESC)
        """;

        try (Connection connection = Database.getConnection();
             Statement statement = connection.createStatement()) {

            statement.executeUpdate(sql);
            statement.executeUpdate(indexSql);

        } catch (SQLException e) {
            throw new RuntimeException("Failed to create messages table", e);
        }
    }

    public Message save(Message message) {
        message.id = UUID.randomUUID().toString();
        message.createdAt = Instant.now().toString();
        message.flagged = false;

        String sql = """
            INSERT INTO messages (
                id,
                conversation_id,
                sender_id,
                content,
                flagged,
                created_at
            )
            VALUES (?, ?, ?, ?, ?, ?)
        """;

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, message.id);
            statement.setString(2, message.conversationId);
            statement.setString(3, message.senderId);
            statement.setString(4, message.content);
            statement.setBoolean(5, message.flagged);
            statement.setString(6, message.createdAt);

            statement.executeUpdate();

            return message;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to save message", e);
        }
    }

    public List<Message> findByConversationId(String conversationId, int page) {
        int pageSize = 50;
        int offset = Math.max(page, 0) * pageSize;

        String sql = """
            SELECT *
            FROM messages
            WHERE conversation_id = ?
            ORDER BY created_at ASC
            LIMIT ? OFFSET ?
        """;

        List<Message> messages = new ArrayList<>();

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, conversationId);
            statement.setInt(2, pageSize);
            statement.setInt(3, offset);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    messages.add(mapResultSetToMessage(resultSet));
                }
            }

            return messages;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find messages by conversation id", e);
        }
    }

    private Message mapResultSetToMessage(ResultSet resultSet) throws SQLException {
        Message message = new Message();

        message.id = resultSet.getString("id");
        message.conversationId = resultSet.getString("conversation_id");
        message.senderId = resultSet.getString("sender_id");
        message.content = resultSet.getString("content");
        message.flagged = resultSet.getBoolean("flagged");
        message.createdAt = resultSet.getString("created_at");

        return message;
    }
}