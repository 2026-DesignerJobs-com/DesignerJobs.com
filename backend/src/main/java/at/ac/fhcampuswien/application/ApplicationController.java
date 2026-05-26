package at.ac.fhcampuswien.application;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// Client ↔ Designer application / hire-on-fit flow
@RestController
public class ApplicationController {

    private final JobApplicationRepository jobApplicationRepository;

    public ApplicationController(JobApplicationRepository jobApplicationRepository) {
        this.jobApplicationRepository = jobApplicationRepository;
    }

    // POST /jobs/{jobId}/apply              → designer submits application
    @PostMapping("/jobs/{jobId}/apply")
    public ResponseEntity<?> apply(
            @PathVariable String jobId,
            @RequestBody JobApplication application,
            Authentication auth) {

        if (auth == null || auth.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error", "not authenticated"
            ));
        }

        if (!isDesigner(auth)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "error", "only designers can apply to jobs"
            ));
        }

        String designerId = auth.getName();
        String coverLetter = application.coverLetter;

        JobApplication savedApplication =
                jobApplicationRepository.create(jobId, designerId, coverLetter);

        return ResponseEntity.status(HttpStatus.CREATED).body(savedApplication);
    }

    // GET  /jobs/{jobId}/applications       → client lists received applications
    @GetMapping("/jobs/{jobId}/applications")
    public ResponseEntity<?> listApplications(
            @PathVariable String jobId,
            Authentication auth) {

        if (auth == null || auth.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error", "not authenticated"
            ));
        }

        return ResponseEntity.ok(
                jobApplicationRepository.findByJobId(jobId)
        );
    }

    // GET  /applications/{id}              → get single application
    @GetMapping("/applications/{id}")
    public ResponseEntity<?> getApplication(
            @PathVariable String id,
            Authentication auth) {

        if (auth == null || auth.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error", "not authenticated"
            ));
        }

        JobApplication application = jobApplicationRepository.findById(id);

        if (application == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "application not found"
            ));
        }

        return ResponseEntity.ok(application);
    }

    // PUT  /applications/{id}/status       → client accepts or rejects { "status": "ACCEPTED"|"REJECTED" }
    @PutMapping("/applications/{id}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable String id,
            @RequestBody Map<String, String> body,
            Authentication auth) {

        if (auth == null || auth.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error", "not authenticated"
            ));
        }

        JobApplication application = jobApplicationRepository.findById(id);

        if (application == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "application not found"
            ));
        }

        String newStatus = body.get("status");

        if (!"ACCEPTED".equals(newStatus) && !"REJECTED".equals(newStatus)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "status must be ACCEPTED or REJECTED"
            ));
        }

        if (!"PENDING".equals(application.status)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "only PENDING applications can be accepted or rejected"
            ));
        }

        JobApplication updatedApplication =
                jobApplicationRepository.updateStatus(id, newStatus);

        return ResponseEntity.ok(updatedApplication);
    }

    // POST /applications/{id}/hire         → client hires designer → triggers contract generation
    @PostMapping("/applications/{id}/hire")
    public ResponseEntity<?> hire(
            @PathVariable String id,
            Authentication auth) {

        if (auth == null || auth.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error", "not authenticated"
            ));
        }

        JobApplication application = jobApplicationRepository.findById(id);

        if (application == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "application not found"
            ));
        }

        if (!"ACCEPTED".equals(application.status)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "only ACCEPTED applications can be hired"
            ));
        }

        JobApplication hiredApplication =
                jobApplicationRepository.updateStatus(id, "HIRED");

        // TODO: trigger contract creation via ContractService once contract/ is implemented.

        return ResponseEntity.ok(hiredApplication);
    }

    private boolean isDesigner(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(authority ->
                        "DESIGNER".equals(authority.getAuthority())
                                || "ROLE_DESIGNER".equals(authority.getAuthority())
                );
    }
}