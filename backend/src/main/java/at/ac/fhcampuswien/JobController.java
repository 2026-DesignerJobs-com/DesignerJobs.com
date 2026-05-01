package at.ac.fhcampuswien;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/jobs")
public class JobController {

    private final JobStorage storage;

    public JobController(JobStorage storage) {
        this.storage = storage;
    }

    // store a new job
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Job store(@RequestBody Job job) {
        job.id = UUID.randomUUID().toString();
        job.createdAt = Instant.now().toString();
        return storage.add(job);
    }

    // search jobs — all params optional
    @GetMapping
    public List<Job> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String budget,
            @RequestParam(required = false) String workMode) {

        return storage.load().stream()
                .filter(j -> q == null || like(j.title, q) || like(j.description, q))
                .filter(j -> category == null || eq(j.category, category))
                .filter(j -> location == null || like(j.location, location))
                .filter(j -> budget == null || eq(j.budget, budget))
                .filter(j -> workMode == null || eq(j.workMode, workMode))
                .collect(Collectors.toList());
    }

    private boolean like(String field, String value) {
        return field != null && field.toLowerCase().contains(value.toLowerCase());
    }

    private boolean eq(String field, String value) {
        return value != null && value.equalsIgnoreCase(field);
    }
}
