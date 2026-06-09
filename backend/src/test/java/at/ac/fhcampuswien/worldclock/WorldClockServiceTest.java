package at.ac.fhcampuswien.worldclock;

import at.ac.fhcampuswien.external.ExternalTimeApiClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorldClockServiceTest {

    @Mock ExternalTimeApiClient externalTimeApiClient;

    @InjectMocks WorldClockService service;

    private JsonNode timeNode(String date, String time, String day) {
        ObjectNode node = new ObjectMapper().createObjectNode();
        node.put("date", date);
        node.put("time", time);
        node.put("dayOfWeek", day);
        return node;
    }

    @Test
    void getWorldClockTimes_returnsFourCities_withMappedFields() {
        when(externalTimeApiClient.getCurrentTimeForTimezone(anyString()))
                .thenReturn(timeNode("2026-06-10", "12:00", "Wednesday"));

        List<WorldClockResponse> result = service.getWorldClockTimes();

        assertThat(result).hasSize(4);
        assertThat(result).extracting(r -> r.city)
                .containsExactly("New York", "London", "Vienna", "Tokyo");
        assertThat(result.get(0).date).isEqualTo("2026-06-10");
        assertThat(result.get(0).dayOfWeek).isEqualTo("Wednesday");
        verify(externalTimeApiClient, times(4)).getCurrentTimeForTimezone(anyString());
    }

    @Test
    void getWorldClockTimes_toleratesMissingFields_withEmptyDefaults() {
        when(externalTimeApiClient.getCurrentTimeForTimezone(anyString()))
                .thenReturn(new ObjectMapper().createObjectNode()); // empty node

        List<WorldClockResponse> result = service.getWorldClockTimes();

        assertThat(result).hasSize(4);
        assertThat(result.get(0).date).isEmpty();
        assertThat(result.get(0).time).isEmpty();
    }
}
