package at.ac.fhcampuswien.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    // Phase 1 — whole team
    // POST /auth/register  → create user (CLIENT or DESIGNER)
    // POST /auth/login     → return JWT token
    // POST /auth/logout    → invalidate session
    // GET  /auth/me        → return current user from token

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthRequest req) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(Map.of("status", "not_implemented"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest req) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(Map.of("status", "not_implemented"));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(Map.of("status", "not_implemented"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me() {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(Map.of("status", "not_implemented"));
    }
}