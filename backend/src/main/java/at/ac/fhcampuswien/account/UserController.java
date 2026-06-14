package at.ac.fhcampuswien.account;

import at.ac.fhcampuswien.Database.Database;
import at.ac.fhcampuswien.auth.UserModel;
import at.ac.fhcampuswien.auth.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// Owner: Yarah — Profiles & Portfolio API (Phase 2)
@RestController
public class UserController {

    private final UserRepository userRepository;

    // Wir holen uns das UserRepository direkt über Spring Dependency Injection
    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
        createPortfolioTableIfNotExists(); // Tabelle für Portfolios generieren
    }

    // --- Hilfsmethode: Model -> Profile Mapping ---
    // Wandelt dein UserModel in ein DesignerProfile um
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

    // --- Hilfsmethode: Portfolio Tabelle ---
    // Da es noch kein PortfolioRepository gibt, legen wir die Tabelle hier kurz selbst an
    private void createPortfolioTableIfNotExists() {
        String sql = """
            CREATE TABLE IF NOT EXISTS portfolio_items (
                id VARCHAR(36) PRIMARY KEY,
                designer_id VARCHAR(255) NOT NULL,
                title VARCHAR(255) NOT NULL,
                description TEXT,
                image_url VARCHAR(500),
                project_url VARCHAR(500),
                tags VARCHAR(1000),
                created_at VARCHAR(50) NOT NULL
            )
        """;
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            System.err.println("Fehler beim Erstellen der Portfolio-Tabelle: " + e.getMessage());
        }
    }

    // ==========================================
    // --- Designer profiles ---
    // ==========================================

    @GetMapping("/designers")
    public ResponseEntity<?> listDesigners(
            @RequestParam(required = false) String skills,
            @RequestParam(required = false) String location) {

        List<DesignerProfile> profiles = new ArrayList<>();
        // Wir suchen alle IDs von Usern mit der Rolle DESIGNER
        String sql = "SELECT id FROM users WHERE role = 'DESIGNER'";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                // Wir nutzen deine bestehende Methode findById, um den kompletten User zu laden
                UserModel user = userRepository.findById(rs.getString("id"));
                if (user != null) {
                    profiles.add(mapToProfile(user));
                }
            }
        } catch (SQLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Datenbank-Fehler"));
        }

        return ResponseEntity.ok(profiles);
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
    public ResponseEntity<?> updateDesignerProfile(
            @PathVariable String id, @RequestBody DesignerProfile profile) {

        UserModel user = userRepository.findById(id); //
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Designer nicht gefunden"));
        }

        // Werte aus dem JSON in dein UserModel übertragen
        user.fullName = profile.displayName;
        user.bio = profile.bio;
        user.skills = profile.skills;
        if (profile.hourlyRate != null) {
            user.hourlyMin = profile.hourlyRate.intValue();
        }
        user.city = profile.location; // Einfachheitshalber speichern wir die Location in city

        // Speichern über deine existierende Update-Methode
        userRepository.update(user);

        return ResponseEntity.ok(mapToProfile(user));
    }

    // ==========================================
    // --- Portfolio ---
    // ==========================================

    @GetMapping("/designers/{id}/portfolio")
    public ResponseEntity<?> getPortfolio(@PathVariable String id) {
        List<PortfolioItem> items = new ArrayList<>();
        String sql = "SELECT * FROM portfolio_items WHERE designer_id = ? ORDER BY created_at DESC";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    PortfolioItem item = new PortfolioItem(); //
                    item.id = rs.getString("id");
                    item.designerId = rs.getString("designer_id");
                    item.title = rs.getString("title");
                    item.description = rs.getString("description");
                    item.imageUrl = rs.getString("image_url");
                    item.projectUrl = rs.getString("project_url");
                    item.tags = rs.getString("tags");
                    item.createdAt = rs.getString("created_at");
                    items.add(item);
                }
            }
        } catch (SQLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Portfolio konnte nicht geladen werden"));
        }

        return ResponseEntity.ok(items);
    }

    @PostMapping("/designers/{id}/portfolio")
    public ResponseEntity<?> addPortfolioItem(
            @PathVariable String id, @RequestBody PortfolioItem item) { //

        if (item.id == null || item.id.isBlank()) item.id = UUID.randomUUID().toString();
        item.designerId = id;
        item.createdAt = Instant.now().toString();

        String sql = """
            INSERT INTO portfolio_items (id, designer_id, title, description, image_url, project_url, tags, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, item.id);
            stmt.setString(2, item.designerId);
            stmt.setString(3, item.title);
            stmt.setString(4, item.description);
            stmt.setString(5, item.imageUrl);
            stmt.setString(6, item.projectUrl);
            stmt.setString(7, item.tags);
            stmt.setString(8, item.createdAt);
            stmt.executeUpdate();
        } catch (SQLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Portfolio Item konnte nicht gespeichert werden"));
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(item);
    }

    @DeleteMapping("/designers/{id}/portfolio/{itemId}")
    public ResponseEntity<?> deletePortfolioItem(
            @PathVariable String id, @PathVariable String itemId) {

        String sql = "DELETE FROM portfolio_items WHERE id = ? AND designer_id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, itemId);
            stmt.setString(2, id);
            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Item nicht gefunden"));
            }
        } catch (SQLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Item konnte nicht gelöscht werden"));
        }

        return ResponseEntity.ok(Map.of("message", "Erfolgreich gelöscht"));
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

        // WICHTIG: Das Passwort entfernen, bevor das Objekt als JSON verschickt wird!
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
        List<at.ac.fhcampuswien.auth.UserModel> users = new java.util.ArrayList<>();
        String sql = "SELECT * FROM users"; // Holt ALLE Einträge aus der Datenbank

        try (Connection conn = at.ac.fhcampuswien.Database.Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                at.ac.fhcampuswien.auth.UserModel user = new at.ac.fhcampuswien.auth.UserModel();
                user.id = rs.getString("id");
                user.fullName = rs.getString("full_name");
                user.email = rs.getString("email");
                user.role = rs.getString("role");
                user.passwordHash = null; // Passwort aus Sicherheitsgründen nie mitsenden!
                users.add(user);
            }
        } catch (SQLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Fehler beim Laden der User-Tabelle: " + e.getMessage()));
        }

        return ResponseEntity.ok(users);
    }
}