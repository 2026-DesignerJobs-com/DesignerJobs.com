package at.ac.fhcampuswien.moderation;

import at.ac.fhcampuswien.infrastructure.Database.Database;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
public class ReportRepository {

    public ReportRepository() {
        createTableIfNotExists();
    }

    private void createTableIfNotExists() {
        String sql = """
            CREATE TABLE IF NOT EXISTS reports (
                id VARCHAR(36) PRIMARY KEY,
                reporter_id VARCHAR(255) NOT NULL,
                target_type VARCHAR(50) NOT NULL,
                target_id VARCHAR(255) NOT NULL,
                reason TEXT NOT NULL,
                status VARCHAR(50) NOT NULL,
                created_at VARCHAR(50) NOT NULL
            )
        """;
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Fehler beim Erstellen der Reports-Tabelle", e);
        }
    }

    public void save(Report report) {
        if (report.id == null) report.id = UUID.randomUUID().toString();
        if (report.status == null) report.status = "OPEN";
        if (report.createdAt == null) report.createdAt = Instant.now().toString();

        String sql = "INSERT INTO reports (id, reporter_id, target_type, target_id, reason, status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, report.id);
            stmt.setString(2, report.reporterId);
            stmt.setString(3, report.targetType);
            stmt.setString(4, report.targetId);
            stmt.setString(5, report.reason);
            stmt.setString(6, report.status);
            stmt.setString(7, report.createdAt);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Fehler beim Speichern des Reports", e);
        }
    }

    public List<Report> findAll() {
        List<Report> list = new ArrayList<>();
        String sql = "SELECT * FROM reports ORDER BY created_at DESC";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToReport(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Fehler beim Laden aller Reports", e);
        }
        return list;
    }

    public Report findById(String id) {
        String sql = "SELECT * FROM reports WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapResultSetToReport(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Fehler beim Suchen des Reports", e);
        }
        return null;
    }

    public void updateStatus(String id, String status) {
        String sql = "UPDATE reports SET status = ? WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setString(2, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Fehler beim Updaten des Report-Status", e);
        }
    }

    private Report mapResultSetToReport(ResultSet rs) throws SQLException {
        Report r = new Report();
        r.id = rs.getString("id");
        r.reporterId = rs.getString("reporter_id");
        r.targetType = rs.getString("target_type");
        r.targetId = rs.getString("target_id");
        r.reason = rs.getString("reason");
        r.status = rs.getString("status");
        r.createdAt = rs.getString("created_at");
        return r;
    }
}