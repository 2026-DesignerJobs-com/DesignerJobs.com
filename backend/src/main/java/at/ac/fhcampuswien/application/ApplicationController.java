package at.ac.fhcampuswien.application;

import at.ac.fhcampuswien.job.Job;
import at.ac.fhcampuswien.job.JobRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// Client ↔ Designer application / hire-on-fit flow
@RestController
public class ApplicationController {

    private final JobApplicationRepository jobApplicationRepository;
    private final JobRepository jobRepository;

    public ApplicationController(JobApplicationRepository jobApplicationRepository,
                                 JobRepository jobRepository) {
        this.jobApplicationRepository = jobApplicationRepository;
        this.jobRepository = jobRepository;
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

        if (jobRepository.findById(jobId) == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "job not found"
            ));
        }

        // Up-front check for a clean 409; the UNIQUE (job_id, designer_id)
        // constraint in the applications table is the backstop.
        if (jobApplicationRepository.existsByJobIdAndDesignerId(jobId, designerId)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "error", "you have already applied to this job"
            ));
        }

        try {
            JobApplication savedApplication =
                    jobApplicationRepository.create(jobId, designerId, application.coverLetter);

            return ResponseEntity.status(HttpStatus.CREATED).body(savedApplication);
        } catch (DuplicateApplicationException e) {
            // The up-front check above races with concurrent applies; the UNIQUE
            // constraint is the backstop, mapped to 409 here.
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "error", "you have already applied to this job"
            ));
        }
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

        Job job = jobRepository.findById(jobId);

        if (job == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "job not found"
            ));
        }

        // Applicant lists are private to the client who posted the job.
        if (job.clientId == null || !job.clientId.equals(auth.getName())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "error", "only the job owner can view its applications"
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

        boolean isApplicant = application.designerId != null
                && application.designerId.equals(auth.getName());

        if (!isApplicant && !isJobOwner(application, auth)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "error", "only the job owner or the applicant can view this application"
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

        if (!isJobOwner(application, auth)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "error", "only the job owner can accept or reject applications"
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
                jobApplicationRepository.updateStatusFrom(id, "PENDING", newStatus);

        if (updatedApplication == null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "error", "application is no longer PENDING"
            ));
        }

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

        if (!isJobOwner(application, auth)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "error", "only the job owner can hire for this job"
            ));
        }

        if (!"ACCEPTED".equals(application.status)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "only ACCEPTED applications can be hired"
            ));
        }

        JobApplication hiredApplication =
                jobApplicationRepository.updateStatusFrom(id, "ACCEPTED", "HIRED");

        if (hiredApplication == null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "error", "application is no longer ACCEPTED"
            ));
        }

        // TODO: trigger contract creation via ContractService once contract/ is implemented.

        return ResponseEntity.ok(hiredApplication);
    }

    private boolean isJobOwner(JobApplication application, Authentication auth) {
        Job job = jobRepository.findById(application.jobId);
        return job != null && job.clientId != null && job.clientId.equals(auth.getName());
    }

    private boolean isDesigner(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(authority ->
                        "DESIGNER".equals(authority.getAuthority())
                                || "ROLE_DESIGNER".equals(authority.getAuthority())
                );
    }
}