package at.ac.fhcampuswien;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/jobs")
public class JobController {

    private final JobRepository jobRepository = new JobRepository();

    // store a new job
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Job store(@RequestBody Job job) {
        job.id = UUID.randomUUID().toString();
        job.createdAt = Instant.now().toString();

        return jobRepository.add(job);
    }

    // search/list jobs — all params optional
    @GetMapping
    public List<Job> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String designType,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String budget,
            @RequestParam(required = false) String workMode,
            @RequestParam(required = false) String tags) {

        return jobRepository.search(
                q,
                category,
                designType,
                location,
                budget,
                workMode,
                tags
        );
    }

    // get one random job
    @GetMapping("/random")
    public ResponseEntity<Job> getRandomJob() {
        Job randomJob = jobRepository.getRandomJob();

        if (randomJob == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(randomJob);
    }

    // get one job by id
    @GetMapping("/{id}")
    public ResponseEntity<Job> getById(@PathVariable String id) {
        Job job = jobRepository.findById(id);

        if (job == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(job);
    }

    // update one job
    @PutMapping("/{id}")
    public ResponseEntity<Job> update(@PathVariable String id, @RequestBody Job updated) {
        Job existingJob = jobRepository.findById(id);

        if (existingJob == null) {
            return ResponseEntity.notFound().build();
        }

        updated.id = id;

        if (updated.createdAt == null) {
            updated.createdAt = existingJob.createdAt;
        }

        Job savedJob = jobRepository.update(id, updated);

        if (savedJob == null) {
            return ResponseEntity.internalServerError().build();
        }

        return ResponseEntity.ok(savedJob);
    }

    // delete one job
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        boolean deleted = jobRepository.deleteById(id);

        if (!deleted) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }
}