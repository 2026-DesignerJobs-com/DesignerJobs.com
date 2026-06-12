package at.ac.fhcampuswien.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@Component
public class ExternalTimeApiClient {

    private static final String BASE_URL = "https://www.timeapi.io/api/Time/current/zone?timeZone=";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ExternalTimeApiClient(ObjectMapper objectMapper) {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = objectMapper;
    }

    public JsonNode getCurrentTimeForTimezone(String timezone) {
        try {
            String encodedTimezone = URLEncoder.encode(timezone, StandardCharsets.UTF_8);
            URI uri = URI.create(BASE_URL + encodedTimezone);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            // Handle non-2xx server responses without throwing exceptions
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                System.err.println("External time API returned status: " + response.statusCode());
                return null;
            }

            return objectMapper.readTree(response.body());

        } catch (IOException e) {
            // Log network or parsing issues safely
            System.err.println("Failed to read response from external time API: " + e.getMessage());
            return null;
        } catch (InterruptedException e) {
            // Restore interrupted status and log
            System.err.println("External time API request was interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
            return null;
        }
    }
}