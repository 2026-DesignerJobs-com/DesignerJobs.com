package at.ac.fhcampuswien.account;

import at.ac.fhcampuswien.infrastructure.session.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
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
        if (req.fullName == null || req.fullName.isBlank()
                || req.email == null || req.email.isBlank()
                || req.password == null || req.password.isBlank()
                || req.role == null || req.role.isBlank()) {

            return ResponseEntity.badRequest().body(Map.of(
                    "error", "fullName, email, password and role are required"
            ));
        }

        String normalizedRole = req.role.trim().toUpperCase(Locale.ROOT);
        if (!normalizedRole.equals("CLIENT") && !normalizedRole.equals("DESIGNER")) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "role must be CLIENT or DESIGNER"
            ));
        }

        String normalizedEmail = req.email.trim().toLowerCase(Locale.ROOT);
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

        UserModel user = userRepository.findByEmail(req.email.trim().toLowerCase(Locale.ROOT));
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

        Map<String, Object> responseData = new HashMap<>();
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
    public ResponseEntity<?> updateProfile(Authentication auth, @Valid @RequestBody ProfileUpdateRequest body) {
        if (auth == null || auth.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "not authenticated"));
        }

        String userId = auth.getName();
        UserModel user = userRepository.findById(userId);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "user not found"));
        }

        if (body.fullName != null) user.fullName = body.fullName.trim();
        if (body.designType != null) user.designType = body.designType.trim();
        if (body.bio != null) user.bio = body.bio.trim();
        if (body.country != null) user.country = body.country;
        if (body.city != null) user.city = body.city;
        if (body.availability != null) user.availability = body.availability;
        if (body.skills != null) user.skills = body.skills.trim();
        if (body.portfolioVisibility != null) user.portfolioVisibility = body.portfolioVisibility;
        if (body.portfolioUrl != null) user.portfolioUrl = body.portfolioUrl.trim();
        if (body.twitter != null) user.twitter = body.twitter.trim();
        if (body.linkedin != null) user.linkedin = body.linkedin.trim();
        if (body.instagram != null) user.instagram = body.instagram.trim();

        if (body.hourlyMin != null) user.hourlyMin = body.hourlyMin;
        if (body.hourlyMax != null) user.hourlyMax = body.hourlyMax;
        if (body.projectMin != null) user.projectMin = body.projectMin;

        userRepository.update(user);

        Map<String, Object> responseData = new HashMap<>();
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

        userRepository.deleteById(userId);

        return ResponseEntity.ok(Map.of("message", "Profile deleted successfully"));
    }
}