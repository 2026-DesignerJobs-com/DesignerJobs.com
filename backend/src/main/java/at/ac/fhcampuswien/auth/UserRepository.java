package at.ac.fhcampuswien.auth;

import at.ac.fhcampuswien.Database.Database;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

@Repository
public class UserRepository {

    public UserRepository() {
        createTableIfNotExists();
    }

    private void createTableIfNotExists() {
        String sql = """
            CREATE TABLE IF NOT EXISTS users (
                id VARCHAR(255) PRIMARY KEY,
                full_name VARCHAR(255),
                email VARCHAR(255) NOT NULL UNIQUE,
                password_hash VARCHAR(255) NOT NULL,
                role VARCHAR(50) NOT NULL,
                design_type VARCHAR(255),
                skills VARCHAR(1000),
                created_at VARCHAR(255) NOT NULL
            )
        """;

        try (Connection connection = Database.getConnection();
             Statement statement = connection.createStatement()) {

            statement.executeUpdate(sql);

        } catch (SQLException e) {
            throw new RuntimeException("Failed to create users table", e);
        }
    }

    public UserModel save(UserModel user) {
        String sql = """
            INSERT INTO users (
                id,
                full_name,
                email,
                password_hash,
                role,
                design_type,
                skills,
                created_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, user.id);
            statement.setString(2, user.fullName);
            statement.setString(3, user.email);
            statement.setString(4, user.passwordHash);
            statement.setString(5, user.role);
            statement.setString(6, user.designType);
            statement.setString(7, user.skills);
            statement.setString(8, user.createdAt);

            statement.executeUpdate();

            return user;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to save user", e);
        }
    }

    public UserModel findByEmail(String email) {
        String sql = """
            SELECT *
            FROM users
            WHERE email = ?
        """;

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, email);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapResultSetToUser(resultSet);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find user by email", e);
        }

        return null;
    }

    public UserModel findById(String id) {
        String sql = """
            SELECT *
            FROM users
            WHERE id = ?
        """;

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapResultSetToUser(resultSet);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find user by id", e);
        }

        return null;
    }

    public boolean existsByEmail(String email) {
        return findByEmail(email) != null;
    }

    private UserModel mapResultSetToUser(ResultSet resultSet) throws SQLException {
        UserModel user = new UserModel();

        user.id = resultSet.getString("id");
        user.fullName = resultSet.getString("full_name");
        user.email = resultSet.getString("email");
        user.passwordHash = resultSet.getString("password_hash");
        user.role = resultSet.getString("role");
        user.designType = resultSet.getString("design_type");
        user.skills = resultSet.getString("skills");
        user.createdAt = resultSet.getString("created_at");

        return user;
    }
}