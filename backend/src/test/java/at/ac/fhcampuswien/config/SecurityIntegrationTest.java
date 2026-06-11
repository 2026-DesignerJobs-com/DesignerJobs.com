package at.ac.fhcampuswien.config;

import at.ac.fhcampuswien.session.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Boots the real Spring Security filter chain (the thing unit tests cannot
 * exercise) and asserts the authorization matrix from {@link SecurityConfig}.
 *
 * The static block points the JDBC layer at an in-memory H2 BEFORE the Spring
 * context (and the repository beans that create tables in their constructors)
 * is built, so this test never touches the real data/ file database.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    static {
        // All @SpringBootTest classes share one cached Spring context, so they
        // must agree on the same in-memory DB (tables are created once at
        // context startup; requests later read db.url per call).
        System.setProperty("db.url", "jdbc:h2:mem:springboottest;DB_CLOSE_DELAY=-1");
        System.setProperty("db.user", "sa");
        System.setProperty("db.password", "");
    }

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;

    @Test
    void getJobs_isPublic() throws Exception {
        mockMvc.perform(get("/jobs"))
                .andExpect(status().isOk());
    }

    @Test
    void getDesigners_isPublic_reachesStub() throws Exception {
        // permitAll on GET /designers/** → request reaches the 501 stub
        // instead of being blocked with 401.
        mockMvc.perform(get("/designers"))
                .andExpect(status().isNotImplemented());
    }

    @Test
    void getJobById_isPublic() throws Exception {
        // reaches the controller (404 for an unknown id) instead of 401
        mockMvc.perform(get("/jobs/no-such-id"))
                .andExpect(status().isNotFound());
    }

    @Test
    void listApplications_withoutToken_is401() throws Exception {
        // nested job sub-resources are NOT covered by the public GET matchers
        mockMvc.perform(get("/jobs/some-id/applications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void postJobs_withoutToken_is401() throws Exception {
        mockMvc.perform(post("/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Logo\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void postJobs_withValidToken_reachesController() throws Exception {
        String token = jwtService.issue("client-1", "CLIENT");

        mockMvc.perform(post("/jobs")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Logo\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void protectedEndpoint_withGarbageToken_is401() throws Exception {
        mockMvc.perform(get("/auth/me")
                        .header("Authorization", "Bearer not.a.real.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_withoutToken_is401() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authRegister_isPublic() throws Exception {
        // unique email per run to avoid the UNIQUE constraint across repeats
        String email = "sec-" + System.nanoTime() + "@test.com";
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Sec Test\",\"email\":\"" + email
                                + "\",\"password\":\"secret123\",\"role\":\"CLIENT\"}"))
                .andExpect(status().isCreated());
    }
}
