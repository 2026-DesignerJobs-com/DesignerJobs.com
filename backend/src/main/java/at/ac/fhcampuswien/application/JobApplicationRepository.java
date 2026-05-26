package at.ac.fhcampuswien.application;

import at.ac.fhcampuswien.Database.Database;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
public class JobApplicationRepository {

    public JobApplicationRepository() {
        createTableIfNotExists();
    }

    private void createTableIfNotExists() {
        String sql = """
            CREATE TABLE IF NOT EXISTS applications (
                id VARCHAR(36) PRIMARY KEY,
                job_id VARCHAR(36) NOT NULL,
                designer_id VARCHAR(36) NOT NULL,
                cover_letter TEXT,
                status VARCHAR(20) NOT NULL,
                applied_at VARCHAR(50) NOT NULL
            )
        """;

        try (Connection connection = Database.getConnection();
             Statement statement = connection.createStatement()) {

            statement.executeUpdate(sql);

        } catch (SQLException e) {
            throw new RuntimeException("Failed to create applications table", e);
        }
    }

    public JobApplication create(String jobId, String designerId, String coverLetter) {
        JobApplication application = new JobApplication();

        application.id = UUID.randomUUID().toString();
        application.jobId = jobId;
        application.designerId = designerId;
        application.coverLetter = coverLetter;
        application.status = "PENDING";
        application.appliedAt = Instant.now().toString();

        String sql = """
            INSERT INTO applications (
                id,
                job_id,
                designer_id,
                cover_letter,
                status,
                applied_at
            )
            VALUES (?, ?, ?, ?, ?, ?)
        """;

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, application.id);
            statement.setString(2, application.jobId);
            statement.setString(3, application.designerId);
            statement.setString(4, application.coverLetter);
            statement.setString(5, application.status);
            statement.setString(6, application.appliedAt);

            statement.executeUpdate();

            return application;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to create job application", e);
        }
    }

    public List<JobApplication> findByJobId(String jobId) {
        String sql = """
            SELECT *
            FROM applications
            WHERE job_id = ?
            ORDER BY applied_at DESC
        """;

        List<JobApplication> applications = new ArrayList<>();

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, jobId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    applications.add(mapResultSetToApplication(resultSet));
                }
            }

            return applications;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find applications by job id", e);
        }
    }

    public JobApplication findById(String id) {
        String sql = """
            SELECT *
            FROM applications
            WHERE id = ?
        """;

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapResultSetToApplication(resultSet);
                }
            }

            return null;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find application by id", e);
        }
    }

    public JobApplication updateStatus(String id, String status) {
        String sql = """
            UPDATE applications
            SET status = ?
            WHERE id = ?
        """;

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, status);
            statement.setString(2, id);

            int updatedRows = statement.executeUpdate();

            if (updatedRows == 0) {
                return null;
            }

            return findById(id);

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update application status", e);
        }
    }

    private JobApplication mapResultSetToApplication(ResultSet resultSet) throws SQLException {
        JobApplication application = new JobApplication();

        application.id = resultSet.getString("id");
        application.jobId = resultSet.getString("job_id");
        application.designerId = resultSet.getString("designer_id");
        application.coverLetter = resultSet.getString("cover_letter");
        application.status = resultSet.getString("status");
        application.appliedAt = resultSet.getString("applied_at");

        return application;
    }
}