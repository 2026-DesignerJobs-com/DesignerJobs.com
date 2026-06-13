package at.ac.fhcampuswien.infrastructure.Database;

import java.sql.Connection;
import java.sql.Statement;


public class DatabaseInitializer {
    public static void init() {
        String sql = """
    CREATE TABLE IF NOT EXISTS jobs (
        id VARCHAR(36) PRIMARY KEY,
        client_id VARCHAR(36),
        title VARCHAR(255) NOT NULL,
        description TEXT,
        category VARCHAR(100),
        design_type VARCHAR(100),
        location VARCHAR(255),
        budget VARCHAR(50),
        work_mode VARCHAR(50),
        deadline VARCHAR(50),
        tags TEXT,
        created_at VARCHAR(50),
        view_count INT DEFAULT 0
    );
        """;

        String addViewCountColumnSql = """
            ALTER TABLE jobs ADD COLUMN IF NOT EXISTS view_count INT DEFAULT 0;
        """;

        try (Connection connection = Database.getConnection();
             Statement statement = connection.createStatement()) {

            statement.execute(sql);
            statement.execute(addViewCountColumnSql);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
