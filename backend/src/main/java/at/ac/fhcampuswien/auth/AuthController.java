package at.ac.fhcampuswien.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@CrossOrigin(origins = {"http://localhost:63342", "http://localhost:63343"})
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository userRepository; // Gives access to the users table in the database

    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // POST /auth/register → creates a new user account
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthRequest req) {
        // Basic validation: email, password and role are required.
        if (req.email == null || req.email.isBlank()
                || req.password == null || req.password.isBlank()
                || req.role == null || req.role.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "email, password and role are required"
            ));
        }

        // Check if the email already exists.
        if (userRepository.existsByEmail(req.email)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "error", "email already exists"
            ));
        }

        UserModel user = new UserModel();
        user.id = UUID.randomUUID().toString();    // Backend creates a unique user ID
        user.email = req.email;                    // Email comes from register.html
        user.passwordHash = req.password;          // Temporary: later this should be a real password hash
        user.role = req.role;                      // CLIENT or DESIGNER
        user.createdAt = Instant.now().toString(); // Backend creates registration timestamp

        UserModel savedUser = userRepository.save(user);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "userId", savedUser.id,
                "email", savedUser.email,
                "role", savedUser.role
        ));
    }

    // POST /auth/login → later this will return a JWT token

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest req) {
        // Basic validation: email and password are required.
        if (req.email == null || req.email.isBlank()
                || req.password == null || req.password.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "email and password are required"
            ));
        }

        // Find the user by email.
        UserModel user = userRepository.findByEmail(req.email);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error", "invalid email or password"
            ));
        }

        // Temporary password check.
        // At the moment passwordHash contains the plain password.
        // Later we will replace this with real password hashing.
        if (!req.password.equals(user.passwordHash)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error", "invalid email or password"
            ));
        }

        AuthResponse response = new AuthResponse();
        response.token = JwtUtil.generateToken(user.id, user.role); // real JWT token
        response.userId = user.id;
        response.role = user.role;

        return ResponseEntity.ok(response);
    }

    // POST /auth/logout → later this can invalidate a session or clear login state
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(Map.of(
                "status", "not_implemented"
        ));
    }

    // GET /auth/me → later this will return the current user from the token
    @GetMapping("/me")
    public ResponseEntity<?> me() {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(Map.of(
                "status", "not_implemented"
        ));
    }
}