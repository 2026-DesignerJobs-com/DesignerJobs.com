package at.ac.fhcampuswien.account;

import at.ac.fhcampuswien.testsupport.H2TestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserRepositoryTest extends H2TestSupport {

    private UserRepository repository;

    @BeforeEach
    void setUp() {
        repository = new UserRepository();
    }

    private UserModel sampleUser(String email) {
        UserModel user = new UserModel();
        user.id = UUID.randomUUID().toString();
        user.fullName = "Test User";
        user.email = email;
        user.passwordHash = "hashed";
        user.role = "CLIENT";
        user.designType = "";
        user.skills = "";
        user.createdAt = Instant.now().toString();
        return user;
    }

    @Test
    void save_thenFindByEmail_roundTrips() {
        UserModel saved = repository.save(sampleUser("a@test.com"));

        UserModel found = repository.findByEmail("a@test.com");
        assertThat(found).isNotNull();
        assertThat(found.id).isEqualTo(saved.id);
        assertThat(found.role).isEqualTo("CLIENT");
    }

    @Test
    void findById_roundTrips() {
        UserModel saved = repository.save(sampleUser("b@test.com"));

        assertThat(repository.findById(saved.id)).isNotNull();
    }

    @Test
    void findByEmail_returnsNull_whenMissing() {
        assertThat(repository.findByEmail("nobody@test.com")).isNull();
    }

    @Test
    void existsByEmail_reflectsPresence() {
        repository.save(sampleUser("c@test.com"));

        assertThat(repository.existsByEmail("c@test.com")).isTrue();
        assertThat(repository.existsByEmail("other@test.com")).isFalse();
    }
}
