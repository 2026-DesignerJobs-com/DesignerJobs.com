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

        // Check if the email already exists.
        if (userRepository.existsByEmail(req.email)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "error", "email already exists"
            ));
        }

        UserModel user = new UserModel();

        user.id = UUID.randomUUID().toString();
        user.fullName = req.fullName.trim();
        user.email = req.email.trim().toLowerCase();
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

        UserModel user = userRepository.findByEmail(req.email);

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

        return ResponseEntity.ok(Map.of(
                "userId", user.id,
                "email", user.email,
                "role", user.role,
                "createdAt", user.createdAt,
                "fullName", user.fullName == null ? "" : user.fullName,
                "designType", user.designType == null ? "" : user.designType,
                "skills", user.skills == null ? "" : user.skills
        ));
    }
}