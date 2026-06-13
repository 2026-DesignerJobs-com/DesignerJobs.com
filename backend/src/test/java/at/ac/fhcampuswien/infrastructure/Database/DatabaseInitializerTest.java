package at.ac.fhcampuswien.infrastructure.Database;

import at.ac.fhcampuswien.testsupport.H2TestSupport;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseInitializerTest extends H2TestSupport {

    @Test
    void init_createsJobsTable() throws Exception {
        DatabaseInitializer.init();

        try (Connection connection = Database.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM jobs")) {

            assertThat(rs.next()).isTrue();
            assertThat(rs.getInt(1)).isEqualTo(0);
        }
    }
}
