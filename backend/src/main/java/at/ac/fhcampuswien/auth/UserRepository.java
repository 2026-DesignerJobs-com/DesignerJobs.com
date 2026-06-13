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
        // Main users table, including all profile fields.
        String sql = """
            CREATE TABLE IF NOT EXISTS users (
                id VARCHAR(255) PRIMARY KEY,
                full_name VARCHAR(255),
                email VARCHAR(255) NOT NULL UNIQUE,
                password_hash VARCHAR(255) NOT NULL,
                role VARCHAR(50) NOT NULL,
                design_type VARCHAR(255),
                bio VARCHAR(2000),
                country VARCHAR(255),
                city VARCHAR(255),
                availability VARCHAR(50),
                hourly_min INT DEFAULT 0,
                hourly_max INT DEFAULT 0,
                project_min INT DEFAULT 0,
                skills VARCHAR(1000),
                portfolio_visibility VARCHAR(50),
                portfolio_url VARCHAR(500),
                twitter VARCHAR(255),
                linkedin VARCHAR(500),
                instagram VARCHAR(255),
                created_at VARCHAR(255) NOT NULL
            )
        """;

        try (Connection connection = Database.getConnection();
             Statement statement = connection.createStatement()) {

            statement.executeUpdate(sql);


            statement.executeUpdate("ALTER TABLE users ADD COLUMN IF NOT EXISTS full_name VARCHAR(255)");
            statement.executeUpdate("ALTER TABLE users ADD COLUMN IF NOT EXISTS design_type VARCHAR(255)");
            statement.executeUpdate("ALTER TABLE users ADD COLUMN IF NOT EXISTS bio VARCHAR(2000)");
            statement.executeUpdate("ALTER TABLE users ADD COLUMN IF NOT EXISTS country VARCHAR(255)");
            statement.executeUpdate("ALTER TABLE users ADD COLUMN IF NOT EXISTS city VARCHAR(255)");
            statement.executeUpdate("ALTER TABLE users ADD COLUMN IF NOT EXISTS availability VARCHAR(50)");
            statement.executeUpdate("ALTER TABLE users ADD COLUMN IF NOT EXISTS hourly_min INT DEFAULT 0");
            statement.executeUpdate("ALTER TABLE users ADD COLUMN IF NOT EXISTS hourly_max INT DEFAULT 0");
            statement.executeUpdate("ALTER TABLE users ADD COLUMN IF NOT EXISTS project_min INT DEFAULT 0");
            statement.executeUpdate("ALTER TABLE users ADD COLUMN IF NOT EXISTS skills VARCHAR(1000)");
            statement.executeUpdate("ALTER TABLE users ADD COLUMN IF NOT EXISTS portfolio_visibility VARCHAR(50)");
            statement.executeUpdate("ALTER TABLE users ADD COLUMN IF NOT EXISTS portfolio_url VARCHAR(500)");
            statement.executeUpdate("ALTER TABLE users ADD COLUMN IF NOT EXISTS twitter VARCHAR(255)");
            statement.executeUpdate("ALTER TABLE users ADD COLUMN IF NOT EXISTS linkedin VARCHAR(500)");
            statement.executeUpdate("ALTER TABLE users ADD COLUMN IF NOT EXISTS instagram VARCHAR(255)");

        } catch (SQLException e) {
            throw new RuntimeException("Failed to create or migrate users table", e);
        }
    }

    public UserModel save(UserModel user) {
        // Covers the case where a user is registered together with profile data.
        String sql = """
            INSERT INTO users (
                id, full_name, email, password_hash, role, design_type, bio, 
                country, city, availability, hourly_min, hourly_max, project_min, 
                skills, portfolio_visibility, portfolio_url, twitter, linkedin, instagram, created_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, user.id);
            statement.setString(2, user.fullName);
            statement.setString(3, user.email);
            statement.setString(4, user.passwordHash);
            statement.setString(5, user.role);
            statement.setString(6, user.designType);
            statement.setString(7, user.bio);
            statement.setString(8, user.country);
            statement.setString(9, user.city);
            statement.setString(10, user.availability);
            statement.setInt(11, user.hourlyMin);
            statement.setInt(12, user.hourlyMax);
            statement.setInt(13, user.projectMin);
            statement.setString(14, user.skills);
            statement.setString(15, user.portfolioVisibility);
            statement.setString(16, user.portfolioUrl);
            statement.setString(17, user.twitter);
            statement.setString(18, user.linkedin);
            statement.setString(19, user.instagram);
            statement.setString(20, user.createdAt);

            statement.executeUpdate();
            return user;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to save user", e);
        }
    }

    public UserModel findByEmail(String email) {
        // Exclude soft-deleted (anonymized) accounts so they can't log in / be fetched (B23).
        String sql = "SELECT * FROM users WHERE email = ? AND role <> 'DELETED'";
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
        // Exclude soft-deleted (anonymized) accounts (B23).
        String sql = "SELECT * FROM users WHERE id = ? AND role <> 'DELETED'";
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

    public void update(UserModel user) {
        String sql = """
            UPDATE users
            SET full_name = ?, 
                design_type = ?, 
                bio = ?, 
                country = ?, 
                city = ?, 
                availability = ?, 
                hourly_min = ?, 
                hourly_max = ?, 
                project_min = ?, 
                skills = ?, 
                portfolio_visibility = ?, 
                portfolio_url = ?, 
                twitter = ?, 
                linkedin = ?, 
                instagram = ?
            WHERE id = ?
        """;

        try (Connection connection = Database.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setString(1, user.fullName);
            stmt.setString(2, user.designType);
            stmt.setString(3, user.bio);
            stmt.setString(4, user.country);
            stmt.setString(5, user.city);
            stmt.setString(6, user.availability);
            stmt.setInt(7, user.hourlyMin);
            stmt.setInt(8, user.hourlyMax);
            stmt.setInt(9, user.projectMin);
            stmt.setString(10, user.skills);
            stmt.setString(11, user.portfolioVisibility);
            stmt.setString(12, user.portfolioUrl);
            stmt.setString(13, user.twitter);
            stmt.setString(14, user.linkedin);
            stmt.setString(15, user.instagram);
            stmt.setString(16, user.id);

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update user profile", e);
        }
    }

    public UserModel updateProfile(String id, UserModel updatedData) {
        updatedData.id = id;
        update(updatedData);
        return findById(id);
    }
    public void deleteById(String id) {

        String sql = """
            UPDATE users
            SET full_name = 'Deleted Account',
                email = 'deleted_' || id || '@designerjobs.com', -- avoids email collisions between deleted accounts
                password_hash = 'NO_ACCESS',
                role = 'DELETED',
                design_type = '',
                bio = '',
                skills = '',
                country = '',
                city = '',
                hourly_min = 0,
                hourly_max = 0,
                project_min = 0,
                portfolio_url = '',
                twitter = '',
                linkedin = '',
                instagram = ''
            WHERE id = ?
        """;

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, id);
            statement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to soft-delete user by id", e);
        }
    }


    private UserModel mapResultSetToUser(ResultSet resultSet) throws SQLException {
        UserModel user = new UserModel();

        user.id = resultSet.getString("id");
        user.fullName = resultSet.getString("full_name");
        user.email = resultSet.getString("email");
        user.passwordHash = resultSet.getString("password_hash");
        user.role = resultSet.getString("role");
        user.designType = resultSet.getString("design_type");
        user.bio = resultSet.getString("bio");
        user.country = resultSet.getString("country");
        user.city = resultSet.getString("city");
        user.availability = resultSet.getString("availability");
        user.hourlyMin = resultSet.getInt("hourly_min");
        user.hourlyMax = resultSet.getInt("hourly_max");
        user.projectMin = resultSet.getInt("project_min");
        user.skills = resultSet.getString("skills");
        user.portfolioVisibility = resultSet.getString("portfolio_visibility");
        user.portfolioUrl = resultSet.getString("portfolio_url");
        user.twitter = resultSet.getString("twitter");
        user.linkedin = resultSet.getString("linkedin");
        user.instagram = resultSet.getString("instagram");
        user.createdAt = resultSet.getString("created_at");

        return user;
    }
}