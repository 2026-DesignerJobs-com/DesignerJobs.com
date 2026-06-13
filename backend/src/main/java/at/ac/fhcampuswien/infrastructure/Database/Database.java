package at.ac.fhcampuswien.infrastructure.Database;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {
    // Defaults preserve the original embedded H2 file DB. Overridable via system
    // properties so tests can point the same repositories at an in-memory H2.
    private static final String DEFAULT_URL = "jdbc:h2:file:./data/projectdb";
    private static final String DEFAULT_USER = "sa";
    private static final String DEFAULT_PASSWORD = "";

    public static Connection getConnection() throws SQLException {
        String url = System.getProperty("db.url", DEFAULT_URL);
        String user = System.getProperty("db.user", DEFAULT_USER);
        String password = System.getProperty("db.password", DEFAULT_PASSWORD);
        return DriverManager.getConnection(url, user, password);
    }
}
