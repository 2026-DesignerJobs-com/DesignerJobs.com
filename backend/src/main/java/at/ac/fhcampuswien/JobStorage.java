package at.ac.fhcampuswien;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// reads and writes jobs.json on disk
@Component
public class JobStorage {

    private static final String FILE = "jobs.json";
    private final ObjectMapper mapper = new ObjectMapper();

    public List<Job> load() {
        File f = new File(FILE);
        if (!f.exists()) return new ArrayList<>();
        try {
            return mapper.readValue(f, new TypeReference<List<Job>>() {});
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    public void save(List<Job> jobs) {
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(FILE), jobs);
        } catch (IOException e) {
            throw new RuntimeException("failed to write jobs.json", e);
        }
    }

    public Job add(Job job) {
        List<Job> jobs = load();
        jobs.add(job);
        save(jobs);
        return job;
    }
}
