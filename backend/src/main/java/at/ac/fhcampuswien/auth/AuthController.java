package at.ac.fhcampuswien.auth;

import at.ac.fhcampuswien.session.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE}) // <-- DAS HIER ERWEITERN/HINZUFÜGEN
public class AuthController {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository,
                          JwtService jwtService,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthRequest req) {

        // Basic validation: fullName, email, password and role are required.
        if (req.fullName == null || req.fullName.isBlank()
                || req.email == null || req.email.isBlank()
                || req.password == null || req.password.isBlank()
                || req.role == null || req.role.isBlank()) {

            return ResponseEntity.badRequest().body(Map.of(
                    "error", "fullName, email, password and role are required"
            ));
        }

        // Normalize role to avoid problems like "designer" instead of "DESIGNER".
        String normalizedRole = req.role.trim().toUpperCase();

        if (!normalizedRole.equals("CLIENT") && !normalizedRole.equals("DESIGNER")) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "role must be CLIENT or DESIGNER"
            ));
        }

        // Emails are stored lower-cased, so the duplicate check must use the
        // normalized value too — otherwise "FOO@x.com" slips past and hits the
        // UNIQUE constraint as a 500 instead of a clean 409.
        String normalizedEmail = req.email.trim().toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "error", "email already exists"
            ));
        }

        UserModel user = new UserModel();

        user.id = UUID.randomUUID().toString();
        user.fullName = req.fullName.trim();
        user.email = normalizedEmail;
        user.passwordHash = passwordEncoder.encode(req.password);
        user.role = normalizedRole;
        user.createdAt = Instant.now().toString();

        // Designer fields are only relevant for DESIGNER accounts.
        if (normalizedRole.equals("DESIGNER")) {
            user.designType = req.designType == null ? "" : req.designType.trim();
            user.skills = req.skills == null ? "" : req.skills.trim();
        } else {
            user.designType = "";
            user.skills = "";
        }

        UserModel saved = userRepository.save(user);

        AuthResponse response = new AuthResponse();
        response.token = jwtService.issue(saved.id, saved.role);
        response.userId = saved.id;
        response.role = saved.role;

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest req) {
        if (req.email == null || req.email.isBlank()
                || req.password == null || req.password.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "email and password are required"
            ));
        }

        // Registration stores emails lower-cased — normalize the same way here,
        // otherwise users who registered with mixed case can never log in.
        UserModel user = userRepository.findByEmail(req.email.trim().toLowerCase());

        if (user == null || !passwordEncoder.matches(req.password, user.passwordHash)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error", "invalid email or password"
            ));
        }

        AuthResponse response = new AuthResponse();
        response.token = jwtService.issue(user.id, user.role);
        response.userId = user.id;
        response.role = user.role;

        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        // Stateless JWT: the client discards the token. Server has nothing to invalidate
        // unless a blacklist is introduced — out of scope for the current iteration.
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error", "not authenticated"
            ));
        }

        String userId = auth.getName();
        UserModel user = userRepository.findById(userId);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error", "user no longer exists"
            ));
        }

        // Wir nutzen eine HashMap, weil Map.of maximal 10 Paare erlaubt
        java.util.Map<String, Object> responseData = new java.util.HashMap<>();
        responseData.put("userId", user.id);
        responseData.put("email", user.email);
        responseData.put("role", user.role);
        responseData.put("createdAt", user.createdAt);
        responseData.put("fullName", user.fullName == null ? "" : user.fullName);
        responseData.put("designType", user.designType == null ? "" : user.designType);
        responseData.put("bio", user.bio == null ? "" : user.bio);
        responseData.put("country", user.country == null ? "" : user.country);
        responseData.put("city", user.city == null ? "" : user.city);
        responseData.put("availability", user.availability == null ? "available" : user.availability);
        responseData.put("hourlyMin", user.hourlyMin);
        responseData.put("hourlyMax", user.hourlyMax);
        responseData.put("projectMin", user.projectMin);
        responseData.put("skills", user.skills == null ? "" : user.skills);
        responseData.put("portfolioVisibility", user.portfolioVisibility == null ? "public" : user.portfolioVisibility);
        responseData.put("portfolioUrl", user.portfolioUrl == null ? "" : user.portfolioUrl);
        responseData.put("twitter", user.twitter == null ? "" : user.twitter);
        responseData.put("linkedin", user.linkedin == null ? "" : user.linkedin);
        responseData.put("instagram", user.instagram == null ? "" : user.instagram);

        return ResponseEntity.ok(responseData);
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateProfile(Authentication auth, @RequestBody java.util.Map<String, Object> body) {
        if (auth == null || auth.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "not authenticated"));
        }

        String userId = auth.getName();
        UserModel user = userRepository.findById(userId);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "user not found"));
        }

        // 1. Textfelder auslesen
        user.fullName = body.containsKey("fullName") ? (String) body.get("fullName") : user.fullName;
        user.designType = body.containsKey("designType") ? (String) body.get("designType") : user.designType;
        user.bio = body.containsKey("bio") ? (String) body.get("bio") : user.bio;
        user.country = body.containsKey("country") ? (String) body.get("country") : user.country;
        user.city = body.containsKey("city") ? (String) body.get("city") : user.city;
        user.availability = body.containsKey("availability") ? (String) body.get("availability") : user.availability;
        user.skills = body.containsKey("skills") ? (String) body.get("skills") : user.skills;
        user.portfolioVisibility = body.containsKey("portfolioVisibility") ? (String) body.get("portfolioVisibility") : user.portfolioVisibility;
        user.portfolioUrl = body.containsKey("portfolioUrl") ? (String) body.get("portfolioUrl") : user.portfolioUrl;
        user.twitter = body.containsKey("twitter") ? (String) body.get("twitter") : user.twitter;
        user.linkedin = body.containsKey("linkedin") ? (String) body.get("linkedin") : user.linkedin;
        user.instagram = body.containsKey("instagram") ? (String) body.get("instagram") : user.instagram;

        // 2. Zahlenfelder auslesen
        if (body.containsKey("hourlyMin") && body.get("hourlyMin") != null) {
            user.hourlyMin = ((Number) body.get("hourlyMin")).intValue();
        }
        if (body.containsKey("hourlyMax") && body.get("hourlyMax") != null) {
            user.hourlyMax = ((Number) body.get("hourlyMax")).intValue();
        }
        if (body.containsKey("projectMin") && body.get("projectMin") != null) {
            user.projectMin = ((Number) body.get("projectMin")).intValue();
        }

        // 3. In der Datenbank speichern
        userRepository.update(user);

        // 4. Antwort via HashMap zusammenbauen (umgeht das 10-Elemente-Limit)
        java.util.Map<String, Object> responseData = new java.util.HashMap<>();
        responseData.put("fullName", user.fullName == null ? "" : user.fullName);
        responseData.put("designType", user.designType == null ? "" : user.designType);
        responseData.put("bio", user.bio == null ? "" : user.bio);
        responseData.put("country", user.country == null ? "" : user.country);
        responseData.put("city", user.city == null ? "" : user.city);
        responseData.put("availability", user.availability == null ? "available" : user.availability);
        responseData.put("hourlyMin", user.hourlyMin);
        responseData.put("hourlyMax", user.hourlyMax);
        responseData.put("projectMin", user.projectMin);
        responseData.put("skills", user.skills == null ? "" : user.skills);
        responseData.put("portfolioVisibility", user.portfolioVisibility == null ? "public" : user.portfolioVisibility);
        responseData.put("portfolioUrl", user.portfolioUrl == null ? "" : user.portfolioUrl);
        responseData.put("twitter", user.twitter == null ? "" : user.twitter);
        responseData.put("linkedin", user.linkedin == null ? "" : user.linkedin);
        responseData.put("instagram", user.instagram == null ? "" : user.instagram);

        return ResponseEntity.ok(responseData);
    }

    @DeleteMapping("/me")
    public ResponseEntity<?> deleteProfile(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "not authenticated"));
        }

        String userId = auth.getName();
        UserModel user = userRepository.findById(userId);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "user not found"));
        }

        // User aus der Datenbank löschen
        userRepository.deleteById(userId);

        // Erfolgsmeldung zurückgeben
        return ResponseEntity.ok(Map.of("message", "Profile deleted successfully"));
    }
}