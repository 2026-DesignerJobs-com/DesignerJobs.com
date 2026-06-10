package at.ac.fhcampuswien.location;

import at.ac.fhcampuswien.external.ExternalLocationApiClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LocationService {

    private final ExternalLocationApiClient externalLocationApiClient;

    public LocationService(ExternalLocationApiClient externalLocationApiClient) {
        this.externalLocationApiClient = externalLocationApiClient;
    }

    public List<String> getSupportedCountries() {
        return List.of(
                "Austria",
                "Germany",
                "Switzerland"
        );
    }

    public List<String> getCitiesByCountry(String country) {
        if (country == null || country.isBlank()) {
            throw new IllegalArgumentException("country is required");
        }

        if (!getSupportedCountries().contains(country)) {
            throw new IllegalArgumentException("country is not supported");
        }

        return externalLocationApiClient.getCitiesByCountry(country);
    }
}