package at.ac.fhcampuswien.integration.worldclock;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Demonstrates external API integration:
// Frontend -> Spring Boot backend -> external Time API.
@RestController
public class WorldClockController {

    private final WorldClockService worldClockService;

    public WorldClockController(WorldClockService worldClockService) {
        this.worldClockService = worldClockService;
    }

    @GetMapping("/world-clock")
    public List<WorldClockResponse> getWorldClock() {
        return worldClockService.getWorldClockTimes();
    }
}