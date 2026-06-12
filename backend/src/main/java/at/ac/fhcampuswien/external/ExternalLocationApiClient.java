package at.ac.fhcampuswien.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class ExternalLocationApiClient {

    private static final String CITIES_API_URL = "https://countriesnow.space/api/v0.1/countries/cities";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ExternalLocationApiClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public List<String> getCitiesByCountry(String country) {
        if (country == null || country.isBlank()) {
            throw new IllegalArgumentException("country is required");
        }

        try {
            String requestBody = objectMapper.writeValueAsString(new CountryRequest(country));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(CITIES_API_URL))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                System.out.println("External location API status: " + response.statusCode());
                System.out.println("External location API body: " + response.body());

                throw new RuntimeException("External location API failed with status: " + response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode data = root.get("data");

            List<String> cities = new ArrayList<>();

            if (data != null && data.isArray()) {
                for (JsonNode cityNode : data) {
                    cities.add(cityNode.asText());
                }
            }

            Collections.sort(cities);

            return cities;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Could not load cities from external location API", e);
        } catch (IOException e) {
            throw new RuntimeException("Could not load cities from external location API", e);
        }
    }

    private record CountryRequest(String country) {
    }
}