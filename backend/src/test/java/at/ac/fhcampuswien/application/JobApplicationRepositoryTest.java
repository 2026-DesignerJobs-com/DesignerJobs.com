package at.ac.fhcampuswien.application;

import at.ac.fhcampuswien.testsupport.H2TestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JobApplicationRepositoryTest extends H2TestSupport {

    private JobApplicationRepository repository;

    @BeforeEach
    void setUp() {
        repository = new JobApplicationRepository();
    }

    @Test
    void create_defaultsStatusToPending_andStampsFields() {
        JobApplication saved = repository.create("job-1", "designer-1", "hire me");

        assertThat(saved.id).isNotBlank();
        assertThat(saved.status).isEqualTo("PENDING");
        assertThat(saved.appliedAt).isNotBlank();
        assertThat(saved.jobId).isEqualTo("job-1");
        assertThat(saved.designerId).isEqualTo("designer-1");
    }

    @Test
    void findByJobId_returnsApplicationsForThatJob() {
        repository.create("job-1", "designer-1", "a");
        repository.create("job-1", "designer-2", "b");
        repository.create("job-2", "designer-3", "c");

        assertThat(repository.findByJobId("job-1")).hasSize(2);
        assertThat(repository.findByJobId("job-2")).hasSize(1);
    }

    @Test
    void findById_roundTripsAndReturnsNullWhenMissing() {
        JobApplication saved = repository.create("job-1", "designer-1", "x");

        assertThat(repository.findById(saved.id)).isNotNull();
        assertThat(repository.findById("missing")).isNull();
    }

    @Test
    void updateStatus_changesStatus_andReturnsUpdatedRow() {
        JobApplication saved = repository.create("job-1", "designer-1", "x");

        JobApplication updated = repository.updateStatus(saved.id, "ACCEPTED");

        assertThat(updated).isNotNull();
        assertThat(updated.status).isEqualTo("ACCEPTED");
    }

    @Test
    void updateStatus_returnsNull_whenIdMissing() {
        assertThat(repository.updateStatus("missing", "ACCEPTED")).isNull();
    }

    /**
     * Characterization test for bug B7: there is no unique constraint on
     * (job_id, designer_id), so the same designer can apply to the same job
     * any number of times. This documents current (buggy) behavior.
     */
    @Test
    void create_allowsDuplicateApplications_documentsMissingConstraint() {
        repository.create("job-1", "designer-1", "first");
        repository.create("job-1", "designer-1", "second");

        assertThat(repository.findByJobId("job-1")).hasSize(2);
    }
}
