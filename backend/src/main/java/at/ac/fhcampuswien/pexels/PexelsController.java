package at.ac.fhcampuswien.pexels;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@CrossOrigin(origins = "*")
@RestController
public class PexelsController {
    private static final String PEXELS_API_KEY = "cGxLEI8BRyoz7roYTh10mdrwBxCnXT8ozUf9MQcD6EhF9SsSF5oh3uCD";

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @GetMapping("/api/design-inspiration")
    public ResponseEntity<String> getDesignInspiration(
            @RequestParam(defaultValue = "graphic design") String query
    ) {
        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);

            String url = "https://api.pexels.com/v1/search"
                    + "?query=" + encodedQuery
                    + "&per_page=6";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", PEXELS_API_KEY)
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            System.out.println("Pexels status: " + response.statusCode());
            System.out.println("Pexels response: " + response.body());

            return ResponseEntity
                    .status(response.statusCode())
                    .body(response.body());

        } catch (Exception error) {
            error.printStackTrace();

            return ResponseEntity
                    .internalServerError()
                    .body("{\"error\":\"Pexels request failed\"}");
        }
    }
    @GetMapping("/api/test")
    public String test() {
        return "Pexels controller is working";
    }
}