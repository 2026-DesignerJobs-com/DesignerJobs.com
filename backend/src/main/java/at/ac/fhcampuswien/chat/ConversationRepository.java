package at.ac.fhcampuswien.chat;

import at.ac.fhcampuswien.Database.Database;
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
public class ConversationRepository {

    public ConversationRepository() {
        createTableIfNotExists();
    }

    private void createTableIfNotExists() {
        String sql = """
            CREATE TABLE IF NOT EXISTS conversations (
                id VARCHAR(255) PRIMARY KEY,
                client_id VARCHAR(255) NOT NULL,
                designer_id VARCHAR(255) NOT NULL,
                job_id VARCHAR(255) NOT NULL,
                created_at VARCHAR(255) NOT NULL,
                UNIQUE (client_id, designer_id, job_id)
            )
        """;

        try (Connection connection = Database.getConnection();
             Statement statement = connection.createStatement()) {

            statement.executeUpdate(sql);

        } catch (SQLException e) {
            throw new RuntimeException("Failed to create conversations table", e);
        }
    }

    public Conversation create(Conversation conversation) {
        Conversation existingConversation = findByParticipantsAndJob(
                conversation.clientId,
                conversation.designerId,
                conversation.jobId
        );

        if (existingConversation != null) {
            return existingConversation;
        }

        conversation.id = UUID.randomUUID().toString();
        conversation.createdAt = Instant.now().toString();

        String sql = """
            INSERT INTO conversations (
                id,
                client_id,
                designer_id,
                job_id,
                created_at
            )
            VALUES (?, ?, ?, ?, ?)
        """;

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, conversation.id);
            statement.setString(2, conversation.clientId);
            statement.setString(3, conversation.designerId);
            statement.setString(4, conversation.jobId);
            statement.setString(5, conversation.createdAt);

            statement.executeUpdate();

            return conversation;

        } catch (SQLException e) {
            // Two simultaneous creates can both pass the lookup above; the loser
            // hits UNIQUE (client_id, designer_id, job_id). Return the winner's
            // row instead of surfacing a 500.
            Conversation winner = findByParticipantsAndJob(
                    conversation.clientId,
                    conversation.designerId,
                    conversation.jobId
            );

            if (winner != null) {
                return winner;
            }

            throw new RuntimeException("Failed to create conversation", e);
        }
    }

    public List<Conversation> findByUserId(String userId) {
        String sql = """
            SELECT *
            FROM conversations
            WHERE client_id = ? OR designer_id = ?
            ORDER BY created_at DESC
        """;

        List<Conversation> conversations = new ArrayList<>();

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, userId);
            statement.setString(2, userId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    conversations.add(mapResultSetToConversation(resultSet));
                }
            }

            return conversations;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find conversations by user id", e);
        }
    }

    public Conversation findById(String id) {
        String sql = """
            SELECT *
            FROM conversations
            WHERE id = ?
        """;

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapResultSetToConversation(resultSet);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find conversation by id", e);
        }

        return null;
    }

    public Conversation findByParticipantsAndJob(String clientId, String designerId, String jobId) {
        String sql = """
            SELECT *
            FROM conversations
            WHERE client_id = ?
              AND designer_id = ?
              AND job_id = ?
        """;

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, clientId);
            statement.setString(2, designerId);
            statement.setString(3, jobId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapResultSetToConversation(resultSet);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find conversation by participants and job", e);
        }

        return null;
    }

    public boolean isParticipant(String conversationId, String userId) {
        Conversation conversation = findById(conversationId);

        if (conversation == null) {
            return false;
        }

        return userId.equals(conversation.clientId) || userId.equals(conversation.designerId);
    }

    private Conversation mapResultSetToConversation(ResultSet resultSet) throws SQLException {
        Conversation conversation = new Conversation();

        conversation.id = resultSet.getString("id");
        conversation.clientId = resultSet.getString("client_id");
        conversation.designerId = resultSet.getString("designer_id");
        conversation.jobId = resultSet.getString("job_id");
        conversation.createdAt = resultSet.getString("created_at");

        return conversation;
    }
}
