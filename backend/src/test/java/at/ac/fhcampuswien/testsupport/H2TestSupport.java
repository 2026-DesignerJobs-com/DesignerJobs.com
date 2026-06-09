package at.ac.fhcampuswien.testsupport;

import org.junit.jupiter.api.BeforeEach;

import java.util.UUID;

/**
 * Base class for repository tests.
 *
 * Points {@code Database.getConnection()} at a fresh in-memory H2 database for
 * every test method by setting the {@code db.url} system property before the
 * repository under test is constructed. {@code DB_CLOSE_DELAY=-1} keeps the
 * schema alive across the open/close-per-call pattern the repositories use; a
 * unique database name per test guarantees full isolation.
 */
public abstract class H2TestSupport {

    @BeforeEach
    void pointDatabaseAtFreshInMemoryDb() {
        String name = "test_" + UUID.randomUUID().toString().replace("-", "");
        System.setProperty("db.url", "jdbc:h2:mem:" + name + ";DB_CLOSE_DELAY=-1");
        System.setProperty("db.user", "sa");
        System.setProperty("db.password", "");
    }
}
