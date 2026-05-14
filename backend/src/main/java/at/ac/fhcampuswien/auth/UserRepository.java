package at.ac.fhcampuswien.auth;

import at.ac.fhcampuswien.Database.Database;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Repository for storing and loading users from the H2 database.
 * This class is used by AuthController for register and login.
 */
@Repository
public class UserRepository {

    public UserRepository() {
        createTableIfNotExists();
    }

    /**
     * Creates the users table if it does not exist yet.
     */
    private void createTableIfNotExists() {
        String sql = """
            CREATE TABLE IF NOT EXISTS users (
                id VARCHAR(255) PRIMARY KEY,
                email VARCHAR(255) NOT NULL UNIQUE,
                password_hash VARCHAR(255) NOT NULL,
                role VARCHAR(50) NOT NULL,
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

    /**
     * Saves a new user in the database.
     */
    public UserModel save(UserModel user) {
        String sql = """
            INSERT INTO users (
                id,
                email,
                password_hash,
                role,
                created_at
            )
            VALUES (?, ?, ?, ?, ?)
        """;

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, user.id);
            statement.setString(2, user.email);
            statement.setString(3, user.passwordHash);
            statement.setString(4, user.role);
            statement.setString(5, user.createdAt);

            statement.executeUpdate();

            return user;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to save user", e);
        }
    }

    /**
     * Finds a user by email.
     * This is needed for login.
     */
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

    /**
     * Checks if an email is already registered.
     */
    public boolean existsByEmail(String email) {
        return findByEmail(email) != null;
    }

    /**
     * Converts one database row into a UserModel object.
     */
    private UserModel mapResultSetToUser(ResultSet resultSet) throws SQLException {
        UserModel user = new UserModel();

        user.id = resultSet.getString("id");
        user.email = resultSet.getString("email");
        user.passwordHash = resultSet.getString("password_hash");
        user.role = resultSet.getString("role");
        user.createdAt = resultSet.getString("created_at");

        return user;
    }
}