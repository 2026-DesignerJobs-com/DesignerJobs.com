package at.ac.fhcampuswien.application;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

// Client ↔ Designer application / hire-on-fit flow
@RestController
public class ApplicationController {

    // POST /jobs/{jobId}/apply              → designer submits application
    // GET  /jobs/{jobId}/applications       → client lists received applications
    // GET  /applications/{id}              → get single application
    // PUT  /applications/{id}/status       → client accepts or rejects { "status": "ACCEPTED"|"REJECTED" }
    // POST /applications/{id}/hire         → client hires designer → triggers contract generation

    @PostMapping("/jobs/{jobId}/apply")
    public ResponseEntity<?> apply(
            @PathVariable String jobId, @RequestBody JobApplication application) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(Map.of("status", "not_implemented"));
    }

    @GetMapping("/jobs/{jobId}/applications")
    public ResponseEntity<?> listApplications(@PathVariable String jobId) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(Map.of("status", "not_implemented"));
    }

    @GetMapping("/applications/{id}")
    public ResponseEntity<?> getApplication(@PathVariable String id) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(Map.of("status", "not_implemented"));
    }

    @PutMapping("/applications/{id}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable String id, @RequestBody Map<String, String> body) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(Map.of("status", "not_implemented"));
    }

    @PostMapping("/applications/{id}/hire")
    public ResponseEntity<?> hire(@PathVariable String id) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(Map.of("status", "not_implemented"));
    }
}