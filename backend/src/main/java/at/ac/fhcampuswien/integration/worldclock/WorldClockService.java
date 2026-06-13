package at.ac.fhcampuswien.integration.worldclock;

import at.ac.fhcampuswien.integration.external.ExternalTimeApiClient;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class WorldClockService {

    private final ExternalTimeApiClient externalTimeApiClient;

    public WorldClockService(ExternalTimeApiClient externalTimeApiClient) {
        this.externalTimeApiClient = externalTimeApiClient;
    }

    public List<WorldClockResponse> getWorldClockTimes() {
        List<WorldClockResponse> worldClockTimes = new ArrayList<>();

        worldClockTimes.add(loadCityTime("New York", "America/New_York"));
        worldClockTimes.add(loadCityTime("London", "Europe/London"));
        worldClockTimes.add(loadCityTime("Vienna", "Europe/Vienna"));
        worldClockTimes.add(loadCityTime("Tokyo", "Asia/Tokyo"));

        return worldClockTimes;
    }

    private WorldClockResponse loadCityTime(String city, String timezone) {
        JsonNode apiResponse = externalTimeApiClient.getCurrentTimeForTimezone(timezone);

        // The client returns null when the upstream API fails or times out — degrade
        // per city instead of NPE-ing the whole /world-clock response.
        if (apiResponse == null) {
            return new WorldClockResponse(city, timezone, "", "", "");
        }

        String date = apiResponse.path("date").asText("");
        String time = apiResponse.path("time").asText("");
        String dayOfWeek = apiResponse.path("dayOfWeek").asText("");

        return new WorldClockResponse(
                city,
                timezone,
                date,
                time,
                dayOfWeek
        );
    }
}