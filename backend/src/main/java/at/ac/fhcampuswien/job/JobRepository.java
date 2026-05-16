package at.ac.fhcampuswien.job;

import at.ac.fhcampuswien.Database.Database;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Repository

public class JobRepository {

    public Job add(Job job) {
        String sql = """
            INSERT INTO jobs (
                id,
                client_id,
                title,
                description,
                category,
                design_type,
                location,
                budget,
                work_mode,
                deadline,
                tags,
                created_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, job.id);
            statement.setString(2, job.clientId);
            statement.setString(3, job.title);
            statement.setString(4, job.description);
            statement.setString(5, job.category);
            statement.setString(6, job.designType);
            statement.setString(7, job.location);
            statement.setString(8, job.budget);
            statement.setString(9, job.workMode);
            statement.setString(10, job.deadline);
            statement.setString(11, job.tags);
            statement.setString(12, job.createdAt);

            statement.executeUpdate();

            return job;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public List<Job> findAll() {
        String sql = """
            SELECT *
            FROM jobs
            ORDER BY created_at DESC
        """;

        List<Job> jobs = new ArrayList<>();

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                jobs.add(mapResultSetToJob(resultSet));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return jobs;
    }

    public Job findById(String id) {
        String sql = """
            SELECT *
            FROM jobs
            WHERE id = ?
        """;

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapResultSetToJob(resultSet);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public Job getRandomJob() {
        String sql = """
            SELECT *
            FROM jobs
            ORDER BY RAND()
            LIMIT 1
        """;

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            if (resultSet.next()) {
                return mapResultSetToJob(resultSet);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public List<Job> search(
            String q,
            String category,
            String designType,
            String location,
            String budget,
            String workMode,
            String tags
    ) {
        String sql = """
            SELECT *
            FROM jobs
            WHERE (? IS NULL OR LOWER(title) LIKE LOWER(?) OR LOWER(description) LIKE LOWER(?))
              AND (? IS NULL OR LOWER(category) = LOWER(?))
              AND (? IS NULL OR LOWER(design_type) = LOWER(?))
              AND (? IS NULL OR LOWER(location) LIKE LOWER(?))
              AND (? IS NULL OR LOWER(budget) = LOWER(?))
              AND (? IS NULL OR LOWER(work_mode) = LOWER(?))
              AND (? IS NULL OR LOWER(tags) LIKE LOWER(?))
            ORDER BY created_at DESC
        """;

        List<Job> jobs = new ArrayList<>();

        String qValue = q == null ? null : "%" + q + "%";
        String locationValue = location == null ? null : "%" + location + "%";
        String tagsValue = tags == null ? null : "%" + tags + "%";

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, q);
            statement.setString(2, qValue);
            statement.setString(3, qValue);

            statement.setString(4, category);
            statement.setString(5, category);

            statement.setString(6, designType);
            statement.setString(7, designType);

            statement.setString(8, location);
            statement.setString(9, locationValue);

            statement.setString(10, budget);
            statement.setString(11, budget);

            statement.setString(12, workMode);
            statement.setString(13, workMode);

            statement.setString(14, tags);
            statement.setString(15, tagsValue);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    jobs.add(mapResultSetToJob(resultSet));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return jobs;
    }

    public Job update(String id, Job updated) {
        String sql = """
            UPDATE jobs
            SET client_id = ?,
                title = ?,
                description = ?,
                category = ?,
                design_type = ?,
                location = ?,
                budget = ?,
                work_mode = ?,
                deadline = ?,
                tags = ?,
                created_at = ?
            WHERE id = ?
        """;

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, updated.clientId);
            statement.setString(2, updated.title);
            statement.setString(3, updated.description);
            statement.setString(4, updated.category);
            statement.setString(5, updated.designType);
            statement.setString(6, updated.location);
            statement.setString(7, updated.budget);
            statement.setString(8, updated.workMode);
            statement.setString(9, updated.deadline);
            statement.setString(10, updated.tags);
            statement.setString(11, updated.createdAt);
            statement.setString(12, id);

            int affectedRows = statement.executeUpdate();

            if (affectedRows > 0) {
                return findById(id);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public boolean deleteById(String id) {
        String sql = """
            DELETE FROM jobs
            WHERE id = ?
        """;

        try (Connection connection = Database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, id);

            int affectedRows = statement.executeUpdate();

            return affectedRows > 0;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    private Job mapResultSetToJob(ResultSet resultSet) throws SQLException {
        Job job = new Job();

        job.id = resultSet.getString("id");
        job.clientId = resultSet.getString("client_id");
        job.title = resultSet.getString("title");
        job.description = resultSet.getString("description");
        job.category = resultSet.getString("category");
        job.designType = resultSet.getString("design_type");
        job.location = resultSet.getString("location");
        job.budget = resultSet.getString("budget");
        job.workMode = resultSet.getString("work_mode");
        job.deadline = resultSet.getString("deadline");
        job.tags = resultSet.getString("tags");
        job.createdAt = resultSet.getString("created_at");

        return job;
    }
}