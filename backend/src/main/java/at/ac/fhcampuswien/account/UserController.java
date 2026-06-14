package at.ac.fhcampuswien.account;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
        userRepository.createPortfolioTableIfNotExists();
    }

    // --- Hilfsmethode: Model -> Profile Mapping ---
    private DesignerProfile mapToProfile(UserModel user) {
        DesignerProfile profile = new DesignerProfile();
        profile.id = user.id;
        profile.userId = user.id;
        profile.displayName = user.fullName;
        profile.bio = user.bio;
        profile.skills = user.skills;
        profile.hourlyRate = user.hourlyMin != 0 ? (double) user.hourlyMin : null;

        // Ort aus City und Country zusammensetzen
        String loc = "";
        if (user.city != null && !user.city.isBlank()) loc += user.city;
        if (user.country != null && !user.country.isBlank()) {
            loc += (loc.isEmpty() ? "" : ", ") + user.country;
        }
        profile.location = loc.isEmpty() ? null : loc;
        return profile;
    }

    // ==========================================
    // --- Designer profiles ---
    // ==========================================

    @GetMapping("/designers")
    public ResponseEntity<?> listDesigners(
            @RequestParam(required = false) String skills,
            @RequestParam(required = false) String location) {

        try {
            // Use the repository to get all users, then filter and map them
            List<DesignerProfile> profiles = userRepository.findAll().stream()
                    .filter(user -> "DESIGNER".equals(user.role))
                    .map(this::mapToProfile)
                    .toList();

            return ResponseEntity.ok(profiles);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Datenbank-Fehler beim Laden der Designer"));
        }
    }

    @GetMapping("/designers/{id}")
    public ResponseEntity<?> getDesignerProfile(@PathVariable String id) {
        UserModel user = userRepository.findById(id); //
        if (user == null || !"DESIGNER".equals(user.role)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Designer nicht gefunden"));
        }
        return ResponseEntity.ok(mapToProfile(user));
    }

    @PutMapping("/designers/{id}")
    public ResponseEntity<?> updateDesignerProfile(@PathVariable String id, @RequestBody DesignerProfile profile) {
        UserModel user = userRepository.findById(id); //
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Designer nicht gefunden"));
        }

        // Werte aus dem JSON inUserModel übertragen
        user.fullName = profile.displayName;
        user.bio = profile.bio;
        user.skills = profile.skills;
        if (profile.hourlyRate != null) {
            user.hourlyMin = profile.hourlyRate.intValue();
        }
        user.city = profile.location;

        userRepository.update(user);

        return ResponseEntity.ok(mapToProfile(user));
    }

    // ==========================================
    // --- Portfolio ---
    // ==========================================

    @GetMapping("/designers/{id}/portfolio")
    public ResponseEntity<?> getPortfolio(@PathVariable String id) {
        try {
            List<PortfolioItem> items = userRepository.findPortfolioByDesignerId(id);
            return ResponseEntity.ok(items);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Portfolio konnte nicht geladen werden"));
        }
    }

    @PostMapping("/designers/{id}/portfolio")
    public ResponseEntity<?> addPortfolioItem(@PathVariable String id, @RequestBody PortfolioItem item) {
        if (item.id == null || item.id.isBlank()) {
            item.id = UUID.randomUUID().toString();
        }
        item.designerId = id;
        item.createdAt = Instant.now().toString();
        try {
            userRepository.savePortfolioItem(item);
            return ResponseEntity.status(HttpStatus.CREATED).body(item);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Portfolio Item konnte nicht gespeichert werden"));
        }
    }

    @DeleteMapping("/designers/{id}/portfolio/{itemId}")
    public ResponseEntity<?> deletePortfolioItem(@PathVariable String id, @PathVariable String itemId) {

        try {
            boolean deleted = userRepository.deletePortfolioItem(itemId, id);
            if (!deleted) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Item nicht gefunden"));
            }
            return ResponseEntity.ok(Map.of("message", "Erfolgreich gelöscht"));
        } catch (Exception e) {

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Item konnte nicht gelöscht werden"));
        }
    }

    // ==========================================
    // --- Generic user ---
    // ==========================================

    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUser(@PathVariable String id) {
        UserModel user = userRepository.findById(id); //

        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User nicht gefunden"));
        }
        user.passwordHash = null;

        return ResponseEntity.ok(user);
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable String id) {
        UserModel user = userRepository.findById(id); //

        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User existiert nicht"));
        }

        userRepository.deleteById(id); //
        return ResponseEntity.ok(Map.of("message", "User erfolgreich gelöscht"));
    }

    @GetMapping("/users")
    public ResponseEntity<?> listAllUsers() {
        try {
            List<UserModel> users = userRepository.findAll();
            users.forEach(user -> user.passwordHash = null);
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Fehler beim Laden der User-Tabelle: " + e.getMessage()));
        }
    }
}