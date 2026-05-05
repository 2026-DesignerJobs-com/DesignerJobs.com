package at.ac.fhcampuswien.user;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

// Owner: Yarah — Profiles & Portfolio API (Phase 2)
@RestController
public class UserController {

    // --- Designer profiles ---

    // GET  /designers              → list designers (filterable by skills, location)
    // GET  /designers/{id}         → get designer profile
    // PUT  /designers/{id}         → update own profile

    @GetMapping("/designers")
    public ResponseEntity<?> listDesigners(
            @RequestParam(required = false) String skills,
            @RequestParam(required = false) String location) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(Map.of("status", "not_implemented"));
    }

    @GetMapping("/designers/{id}")
    public ResponseEntity<?> getDesignerProfile(@PathVariable String id) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(Map.of("status", "not_implemented"));
    }

    @PutMapping("/designers/{id}")
    public ResponseEntity<?> updateDesignerProfile(
            @PathVariable String id, @RequestBody DesignerProfile profile) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(Map.of("status", "not_implemented"));
    }

    // --- Portfolio ---

    // GET    /designers/{id}/portfolio           → list portfolio items
    // POST   /designers/{id}/portfolio           → add portfolio item
    // DELETE /designers/{id}/portfolio/{itemId}  → remove portfolio item

    @GetMapping("/designers/{id}/portfolio")
    public ResponseEntity<?> getPortfolio(@PathVariable String id) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(Map.of("status", "not_implemented"));
    }

    @PostMapping("/designers/{id}/portfolio")
    public ResponseEntity<?> addPortfolioItem(
            @PathVariable String id, @RequestBody PortfolioItem item) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(Map.of("status", "not_implemented"));
    }

    @DeleteMapping("/designers/{id}/portfolio/{itemId}")
    public ResponseEntity<?> deletePortfolioItem(
            @PathVariable String id, @PathVariable String itemId) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(Map.of("status", "not_implemented"));
    }

    // --- Generic user ---

    // GET    /users/{id}  → get user (public fields only)
    // DELETE /users/{id}  → delete own account

    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUser(@PathVariable String id) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(Map.of("status", "not_implemented"));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable String id) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(Map.of("status", "not_implemented"));
    }
}