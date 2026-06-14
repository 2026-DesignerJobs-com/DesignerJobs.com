package at.ac.fhcampuswien.moderation;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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

    private final ReportRepository reportRepository;

    // Dependency Injection über den Konstruktor
    public ModerationController(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    // 1. Nachricht melden
    @PostMapping("/messages/{id}/report")
    public ResponseEntity<?> reportMessage(@PathVariable String id, @RequestBody Report report) {
        report.targetType = "MESSAGE";
        report.targetId = id;
        if (report.reason == null || report.reason.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Reason is required"));
        }
        reportRepository.save(report);
        return ResponseEntity.ok(Map.of("message", "Message reported successfully", "id", report.id));
    }

    // 2. Job-Ausschreibung melden
@PostMapping("/jobs/{id}/report")
public ResponseEntity<?> reportJob(@PathVariable String id, @RequestBody Report report) {
    report.targetType = "JOB";
    report.targetId = id;

    // Wenn das Frontend eine reporterId mitgeschickt hat (z.B. die UUID), behalten wir sie!
    // Nur wenn sie null oder leer ist, setzen wir "anonymous"
    if (report.reporterId == null || report.reporterId.isBlank()) {
        report.reporterId = "anonymous";
    }

    if (report.reason == null || report.reason.isBlank()) {
        return ResponseEntity.badRequest().body(Map.of("error", "Reason is required"));
    }

    reportRepository.save(report);
    return ResponseEntity.ok(Map.of("message", "Job reported successfully", "id", report.id));
}
    // 3. Benutzer/Profil melden
    @PostMapping("/users/{id}/report")
    public ResponseEntity<?> reportUser(@PathVariable String id, @RequestBody Report report) {
        report.targetType = "USER";
        report.targetId = id;
        if (report.reason == null || report.reason.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Reason is required"));
        }
        reportRepository.save(report);
        return ResponseEntity.ok(Map.of("message", "User reported successfully", "id", report.id));
    }

    // 4. Admin-Funktion: Alle Reports auflisten
    @GetMapping("/reports")
    public ResponseEntity<?> listReports(@RequestParam(required = false) String status) {
        List<Report> reports = reportRepository.findAll();

        // Optionaler Filter, falls im Frontend mal ?status=OPEN genutzt wird
        if (status != null && !status.isBlank()) {
            reports.removeIf(r -> !r.status.equalsIgnoreCase(status));
        }

        return ResponseEntity.ok(reports);
    }

    // 5. Admin-Funktion: Status ändern (Verknüpft mit deiner toggleReportStatus JS-Funktion)
    @PutMapping("/reports/{id}")
    public ResponseEntity<?> resolveReport(@PathVariable String id, @RequestBody Map<String, String> body) {
        String newStatus = body.get("status");

        if (newStatus == null || (!newStatus.equals("RESOLVED") && !newStatus.equals("DISMISSED") && !newStatus.equals("OPEN"))) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid status. Must be OPEN, RESOLVED or DISMISSED"));
        }

        Report existingReport = reportRepository.findById(id);
        if (existingReport == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Report nicht gefunden"));
        }

        // Status in der Datenbank überschreiben
        reportRepository.updateStatus(id, newStatus);

        return ResponseEntity.ok(Map.of("message", "Report status updated to " + newStatus));
    }
}