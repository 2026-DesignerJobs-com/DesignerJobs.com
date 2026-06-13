package at.ac.fhcampuswien.integration.location;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/locations")
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping("/countries")
    public List<String> getCountries() {
        return locationService.getSupportedCountries();
    }

    @GetMapping("/cities")
    public ResponseEntity<?> getCities(@RequestParam String country) {
        try {
            return ResponseEntity.ok(locationService.getCitiesByCountry(country));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "error", e.getMessage()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                    "error", "external location API could not be reached"
            ));
        }
    }
}

