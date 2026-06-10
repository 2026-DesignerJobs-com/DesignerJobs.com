package at.ac.fhcampuswien.job;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for job listings.
 *
 * Responsibilities:
 * - create a job as logged-in CLIENT
 * - list/search all jobs
 * - load one job by id for job-detail.html
 *
 * Important for messaging:
 * The clientId is set from Authentication, not from the frontend.
 * This allows job-detail.html to open chat with the correct client.
 */
@RestController
@RequestMapping("/jobs")
public class JobController {

    private final JobRepository jobRepository;

    public JobController(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    /**
     * POST /jobs
     * Creates a new job listing.
     *
     * The logged-in user is the client who owns the job.
     */
    @PostMapping
    public ResponseEntity<?> create(@RequestBody Job job, Authentication auth) {
        if (auth == null || auth.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error", "not authenticated"
            ));
        }

        if (job.title == null || job.title.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "title is required"
            ));
        }

        job.clientId = auth.getName();

        Job savedJob = jobRepository.create(job);

        return ResponseEntity.status(HttpStatus.CREATED).body(savedJob);
    }

    /**
     * GET /jobs
     * Lists or searches jobs.
     */
    @GetMapping
    public List<Job> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String designType,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String budget,
            @RequestParam(required = false) String workMode,
            @RequestParam(required = false) String tags) {

        return jobRepository.search(q, category, designType, location, budget, workMode, tags);
    }

    /**
     * GET /jobs/{id}
     * Loads one job for job-detail.html.
     *
     * This endpoint is important because job-detail.html needs:
     * - job id
     * - title
     * - description
     * - clientId for Message Client
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable String id) {
        Job job = jobRepository.findById(id);

        if (job == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "job not found"
            ));
        }

        return ResponseEntity.ok(job);
    }
    /**
     * DELETE /jobs/{id}
     * Deletes a job.
     *
     * The logged-in client who created the job may delete it.
     * A logged-in designer may also delete it.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteJob(@PathVariable String id, Authentication auth) {
        if (auth == null || auth.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "error", "not authenticated"
            ));
        }

        Job existingJob = jobRepository.findById(id);

        if (existingJob == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "job not found"
            ));
        }

        boolean isOwnerClient = existingJob.clientId != null && existingJob.clientId.equals(auth.getName());

        boolean isDesigner = auth.getAuthorities().stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("DESIGNER")
                                || authority.getAuthority().equals("ROLE_DESIGNER")
                );

        if (!isOwnerClient && !isDesigner) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "error", "only the client who created this job or a designer can delete it"
            ));
        }

        boolean deleted = jobRepository.deleteById(id);

        if (!deleted) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "job could not be deleted"
            ));
        }

        return ResponseEntity.ok(Map.of(
                "message", "job deleted successfully"
        ));
    }
}