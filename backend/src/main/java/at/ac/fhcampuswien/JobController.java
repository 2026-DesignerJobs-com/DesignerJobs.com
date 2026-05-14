package at.ac.fhcampuswien;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@CrossOrigin(origins = {"http://localhost:63342", "http://localhost:63343"})
@RequestMapping("/jobs")
public class JobController {

    private final JobRepository jobRepository; // Uses the database repository instead of the old JSON storage

    public JobController(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    // Stores a new job in the database
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Job store(@RequestBody Job job) {
        job.id = UUID.randomUUID().toString();      // Backend generates a unique ID
        job.createdAt = Instant.now().toString();   // Backend generates the creation timestamp

        return jobRepository.add(job);              // Saves the job in the database
    }

    // Searches or lists jobs from the database; all query parameters are optional
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

    // Returns one job by its ID
    @GetMapping("/{id}")
    public ResponseEntity<Job> getById(@PathVariable String id) {
        Job job = jobRepository.findById(id);

        if (job == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(job);
    }

    // Updates an existing job in the database
    @PutMapping("/{id}")
    public ResponseEntity<Job> update(@PathVariable String id, @RequestBody Job updated) {
        Job existingJob = jobRepository.findById(id);

        if (existingJob == null) {
            return ResponseEntity.notFound().build();
        }

        updated.id = id;
        updated.createdAt = existingJob.createdAt;

        Job savedJob = jobRepository.update(id, updated);

        if (savedJob == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        return ResponseEntity.ok(savedJob);
    }

    // Deletes an existing job from the database
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        boolean deleted = jobRepository.deleteById(id);

        if (!deleted) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }
}