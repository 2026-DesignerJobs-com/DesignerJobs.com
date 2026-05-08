package at.ac.fhcampuswien.moderation;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

// Owner: Lika — AI moderation & reporting (Phase 2)
@RestController
@RequestMapping("/moderation")
public class ModerationController {

    // POST /moderation/messages/{id}/report  → flag a chat message
    // POST /moderation/jobs/{id}/report      → flag a job listing
    // POST /moderation/users/{id}/report     → flag a user
    // GET  /moderation/reports               → admin: list all reports (filter by ?status=OPEN)
    // PUT  /moderation/reports/{id}          → admin: resolve/dismiss { "status": "RESOLVED"|"DISMISSED" }

    @PostMapping("/messages/{id}/report")
    public ResponseEntity<?> reportMessage(
            @PathVariable String id, @RequestBody Report report) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(Map.of("status", "not_implemented"));
    }

    @PostMapping("/jobs/{id}/report")
    public ResponseEntity<?> reportJob(
            @PathVariable String id, @RequestBody Report report) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(Map.of("status", "not_implemented"));
    }

    @PostMapping("/users/{id}/report")
    public ResponseEntity<?> reportUser(
            @PathVariable String id, @RequestBody Report report) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(Map.of("status", "not_implemented"));
    }

    @GetMapping("/reports")
    public ResponseEntity<?> listReports(
            @RequestParam(required = false) String status) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(Map.of("status", "not_implemented"));
    }

    @PutMapping("/reports/{id}")
    public ResponseEntity<?> resolveReport(
            @PathVariable String id, @RequestBody Map<String, String> body) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(Map.of("status", "not_implemented"));
    }
}