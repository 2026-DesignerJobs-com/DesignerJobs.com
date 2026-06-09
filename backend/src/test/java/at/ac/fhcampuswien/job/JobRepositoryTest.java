package at.ac.fhcampuswien.job;

import at.ac.fhcampuswien.testsupport.H2TestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JobRepositoryTest extends H2TestSupport {

    private JobRepository repository;

    @BeforeEach
    void setUp() {
        repository = new JobRepository();
    }

    private Job sampleJob(String title) {
        Job job = new Job();
        job.clientId = "client-1";
        job.title = title;
        job.description = "Need a great " + title;
        job.category = "Graphic Design";
        job.designType = "logo";
        job.location = "remote";
        job.budget = "€€";
        job.workMode = "remote";
        job.deadline = "2026-06-15";
        job.tags = "logo, branding";
        return job;
    }

    @Test
    void create_generatesIdAndTimestamp_andPersists() {
        Job saved = repository.create(sampleJob("Logo Design"));

        assertThat(saved.id).isNotBlank();
        assertThat(saved.createdAt).isNotBlank();

        Job found = repository.findById(saved.id);
        assertThat(found).isNotNull();
        assertThat(found.title).isEqualTo("Logo Design");
        assertThat(found.clientId).isEqualTo("client-1");
    }

    @Test
    void findById_returnsNull_whenMissing() {
        assertThat(repository.findById("does-not-exist")).isNull();
    }

    @Test
    void findAll_returnsEveryJob() {
        repository.create(sampleJob("A"));
        repository.create(sampleJob("B"));

        assertThat(repository.findAll()).hasSize(2);
    }

    @Test
    void search_byKeyword_matchesTitleCaseInsensitively() {
        repository.create(sampleJob("Brand Identity"));
        repository.create(sampleJob("Motion Graphics"));

        List<Job> result = repository.search("brand", null, null, null, null, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).title).isEqualTo("Brand Identity");
    }

    @Test
    void search_withAllNullFilters_returnsEverything() {
        repository.create(sampleJob("A"));
        repository.create(sampleJob("B"));

        assertThat(repository.search(null, null, null, null, null, null, null)).hasSize(2);
    }

    @Test
    void search_byCategory_isExactMatch() {
        repository.create(sampleJob("A"));

        assertThat(repository.search(null, "Graphic Design", null, null, null, null, null)).hasSize(1);
        assertThat(repository.search(null, "Web Design", null, null, null, null, null)).isEmpty();
    }

    @Test
    void getRandomJob_returnsARow_whenJobsExist() {
        repository.create(sampleJob("Only Job"));

        Job random = repository.getRandomJob();

        assertThat(random).isNotNull();
        assertThat(random.title).isEqualTo("Only Job");
    }

    @Test
    void getRandomJob_returnsNull_whenEmpty() {
        assertThat(repository.getRandomJob()).isNull();
    }

    @Test
    void update_changesFields_andReturnsUpdatedRow() {
        Job saved = repository.create(sampleJob("Old Title"));

        Job changes = sampleJob("New Title");
        changes.clientId = saved.clientId;
        changes.createdAt = saved.createdAt;

        Job updated = repository.update(saved.id, changes);

        assertThat(updated).isNotNull();
        assertThat(updated.title).isEqualTo("New Title");
    }

    @Test
    void update_returnsNull_whenIdMissing() {
        assertThat(repository.update("missing", sampleJob("X"))).isNull();
    }

    @Test
    void deleteById_removesRow_andReportsSuccess() {
        Job saved = repository.create(sampleJob("Doomed"));

        assertThat(repository.deleteById(saved.id)).isTrue();
        assertThat(repository.findById(saved.id)).isNull();
    }

    @Test
    void deleteById_returnsFalse_whenMissing() {
        assertThat(repository.deleteById("missing")).isFalse();
    }
}
