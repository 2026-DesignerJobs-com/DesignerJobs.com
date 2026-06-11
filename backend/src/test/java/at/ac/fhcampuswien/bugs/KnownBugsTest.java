package at.ac.fhcampuswien.bugs;

import at.ac.fhcampuswien.application.ApplicationController;
import at.ac.fhcampuswien.application.JobApplicationRepository;
import at.ac.fhcampuswien.auth.AuthController;
import at.ac.fhcampuswien.auth.AuthRequest;
import at.ac.fhcampuswien.auth.UserRepository;
import at.ac.fhcampuswien.job.Job;
import at.ac.fhcampuswien.job.JobRepository;
import at.ac.fhcampuswien.session.JwtService;
import at.ac.fhcampuswien.testsupport.H2TestSupport;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD regression tests for the known bugs in PROJECT_REVIEW.md §9.
 *
 * Each test asserts the CORRECT behavior, so it is RED until the bug is fixed.
 * That's intentional: this is the team's executable to-do list. When every test
 * here is green, every listed bug is fixed. Run with the normal `mvn test`.
 */
class KnownBugsTest extends H2TestSupport {

    // ---- collaborators wired by hand against the in-memory H2 ----

    private JwtService realJwtService() {
        String secret = "designer-jobs-development-secret-key-please-change-me-32";
        SecretKey key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        OctetSequenceKey jwk = new OctetSequenceKey.Builder(key.getEncoded()).build();
        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(jwk)));
        return new JwtService(encoder, 7_200_000L);
    }

    private AuthController realAuthController() {
        return new AuthController(new UserRepository(), realJwtService(), new BCryptPasswordEncoder());
    }

    private AuthRequest registration(String email) {
        AuthRequest req = new AuthRequest();
        req.fullName = "Jane Doe";
        req.email = email;
        req.password = "secret123";
        req.role = "CLIENT";
        return req;
    }

    // ---- B1: login must be case-insensitive on email ----

    @Test
    void b1_loginShouldSucceedRegardlessOfEmailCase() {
        AuthController controller = realAuthController();
        controller.register(registration("Mixed@Test.com")); // stored lower-cased

        AuthRequest login = new AuthRequest();
        login.email = "Mixed@Test.com"; // same case the user typed at registration
        login.password = "secret123";

        ResponseEntity<?> response = controller.login(login);

        // Today: 401 — login queries the raw mixed-case email, but the stored
        // value was lower-cased. After normalizing email, this should be 200.
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // ---- B5: duplicate email with different casing must be a clean 409 ----

    @Test
    void b5_duplicateEmailDifferentCaseShouldReturn409NotCrash() {
        AuthController controller = realAuthController();
        controller.register(registration("foo@x.com")); // stored foo@x.com

        // Today: existsByEmail("FOO@x.com") misses (stored is lower-case), the
        // INSERT then hits the UNIQUE constraint and throws → HTTP 500 / exception.
        // After normalizing email, the duplicate should be caught up-front as 409.
        ResponseEntity<?> response = controller.register(registration("FOO@x.com"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    // ---- B2: only the job owner may list its applications ----

    @Test
    void b2_listApplicationsByNonOwnerShouldBeForbidden() {
        JobRepository jobRepository = new JobRepository();
        Job job = new Job();
        job.clientId = "the-owner";
        job.title = "Logo Design";
        Job savedJob = jobRepository.create(job);

        JobApplicationRepository applicationRepository = new JobApplicationRepository();
        applicationRepository.create(savedJob.id, "designer-1", "hello");

        ApplicationController controller =
                new ApplicationController(applicationRepository, jobRepository);

        ResponseEntity<?> nonOwnerResponse = controller.listApplications(
                savedJob.id,
                new UsernamePasswordAuthenticationToken("not-the-owner", null, List.of()));

        assertThat(nonOwnerResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // The legitimate owner must still see the applicant list.
        ResponseEntity<?> ownerResponse = controller.listApplications(
                savedJob.id,
                new UsernamePasswordAuthenticationToken("the-owner", null, List.of()));

        assertThat(ownerResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // ---- B7: a designer must not apply to the same job twice ----

    @Test
    void b7_duplicateApplicationsShouldNotBeStored() {
        JobApplicationRepository repository = new JobApplicationRepository();

        repository.create("job-1", "designer-1", "first");
        try {
            repository.create("job-1", "designer-1", "second");
        } catch (RuntimeException expectedOnceUniqueConstraintExists) {
            // After the fix a unique (job_id, designer_id) constraint may reject this.
        }

        // Today: 2 rows — no uniqueness. After the fix: at most 1.
        assertThat(repository.findByJobId("job-1")).hasSize(1);
    }
}
