package at.ac.fhcampuswien.infrastructure.config;

import at.ac.fhcampuswien.infrastructure.session.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;

/**
 * C2: the API serves JSON (default) and XML via content negotiation.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ContentNegotiationTest {

    static {
        // Must match the other @SpringBootTest classes — they share one cached
        // Spring context (see SecurityIntegrationTest).
        System.setProperty("db.url", "jdbc:h2:mem:springboottest;DB_CLOSE_DELAY=-1");
        System.setProperty("db.user", "sa");
        System.setProperty("db.password", "");
    }

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;

    @Test
    void jobs_defaultsToJson_withoutAcceptHeader() throws Exception {
        mockMvc.perform(get("/jobs"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void jobs_returnsJson_whenAcceptJson() throws Exception {
        mockMvc.perform(get("/jobs").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void jobs_returnsXml_whenAcceptXml() throws Exception {
        mockMvc.perform(get("/jobs").accept(MediaType.APPLICATION_XML))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML));
    }

    @Test
    void jobById_returnsXmlBody_whenAcceptXml() throws Exception {
        String token = jwtService.issue("client-1", "CLIENT");
        String body = mockMvc.perform(post("/jobs")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Xml Job\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = new ObjectMapper().readTree(body).get("id").asText();

        mockMvc.perform(get("/jobs/" + id).accept(MediaType.APPLICATION_XML))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                .andExpect(xpath("/Job/title").string("Xml Job"));
    }
}
