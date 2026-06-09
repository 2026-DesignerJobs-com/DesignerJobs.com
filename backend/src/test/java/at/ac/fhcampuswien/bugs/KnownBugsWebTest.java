package at.ac.fhcampuswien.bugs;

import at.ac.fhcampuswien.session.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TDD routing-level regression tests (PROJECT_REVIEW.md §9). RED until the
 * endpoints exist / are reachable. Runs in the normal `mvn test`.
 */
@SpringBootTest
@AutoConfigureMockMvc
class KnownBugsWebTest {

    static {
        // Must match the other @SpringBootTest classes — they share one cached
        // Spring context (see SecurityIntegrationTest).
        System.setProperty("db.url", "jdbc:h2:mem:springboottest;DB_CLOSE_DELAY=-1");
        System.setProperty("db.user", "sa");
        System.setProperty("db.password", "");
    }

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;

    private String createJobAndReturnId() throws Exception {
        String token = jwtService.issue("client-1", "CLIENT");
        String body = mockMvc.perform(post("/jobs")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Logo\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return new ObjectMapper().readTree(body).get("id").asText();
    }

    // ---- B4: GET /jobs/random should return a random job ----

    @Test
    void b4_getRandomJobShouldBeReachable() throws Exception {
        createJobAndReturnId();

        // Today: 404 — "/jobs/random" is captured by @GetMapping("/{id}") with
        // id="random" → findById("random") → not found. Needs a dedicated route
        // declared ABOVE the /{id} mapping.
        mockMvc.perform(get("/jobs/random"))
                .andExpect(status().isOk());
    }

    // ---- B3: PUT /jobs/{id} should update an owned job ----

    @Test
    void b3_putJobShouldUpdateExistingJob() throws Exception {
        String id = createJobAndReturnId();
        String token = jwtService.issue("client-1", "CLIENT");

        // Today: 405 Method Not Allowed — JobController declares no @PutMapping,
        // even though JobRepository.update() exists. Needs a controller method.
        mockMvc.perform(put("/jobs/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Updated Title\"}"))
                .andExpect(status().isOk());
    }

    // ---- B3: DELETE /jobs/{id} should remove an owned job ----

    @Test
    void b3_deleteJobShouldRemoveExistingJob() throws Exception {
        String id = createJobAndReturnId();
        String token = jwtService.issue("client-1", "CLIENT");

        // Today: 405 Method Not Allowed — JobController declares no @DeleteMapping,
        // even though JobRepository.deleteById() exists.
        mockMvc.perform(delete("/jobs/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is2xxSuccessful());
    }
}
