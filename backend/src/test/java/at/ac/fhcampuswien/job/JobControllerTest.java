package at.ac.fhcampuswien.job;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobControllerTest {

    @Mock JobRepository jobRepository;
    @Mock Authentication auth;

    @InjectMocks JobController controller;

    @Test
    void create_rejectsUnauthenticated_with401() {
        Job job = new Job();
        job.title = "Logo";

        ResponseEntity<?> response = controller.create(job, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(jobRepository, never()).create(any());
    }

    @Test
    void create_rejectsMissingTitle_with400() {
        when(auth.getName()).thenReturn("client-1");
        Job job = new Job();

        ResponseEntity<?> response = controller.create(job, auth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(jobRepository, never()).create(any());
    }

    @Test
    void create_setsClientIdFromAuth_notFromBody() {
        when(auth.getName()).thenReturn("client-1");
        when(jobRepository.create(any())).thenAnswer(inv -> inv.getArgument(0));

        Job job = new Job();
        job.title = "Logo";
        job.clientId = "SPOOFED";   // must be overwritten

        ResponseEntity<?> response = controller.create(job, auth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(((Job) response.getBody()).clientId).isEqualTo("client-1");
    }

    @Test
    void getById_returns404_whenMissing() {
        when(jobRepository.findById("missing")).thenReturn(null);

        ResponseEntity<?> response = controller.getById("missing");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getById_returnsJob_whenFound() {
        Job job = new Job();
        job.id = "job-1";
        job.title = "Logo";
        when(jobRepository.findById("job-1")).thenReturn(job);

        ResponseEntity<?> response = controller.getById("job-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Job) response.getBody()).id).isEqualTo("job-1");
    }

    @Test
    void deleteJob_returns404_whenMissing() {
        when(auth.getName()).thenReturn("client-1");
        when(jobRepository.findById("missing")).thenReturn(null);

        ResponseEntity<?> response = controller.deleteJob("missing", auth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(jobRepository, never()).deleteById(any());
    }

    @Test
    void deleteJob_rejectsNonOwner_with403() {
        when(auth.getName()).thenReturn("designer-1");
        Job job = new Job();
        job.id = "job-1";
        job.clientId = "the-owner";
        when(jobRepository.findById("job-1")).thenReturn(job);

        ResponseEntity<?> response = controller.deleteJob("job-1", auth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(jobRepository, never()).deleteById(any());
    }

    @Test
    void deleteJob_allowsOwnerClient() {
        when(auth.getName()).thenReturn("the-owner");
        Job job = new Job();
        job.id = "job-1";
        job.clientId = "the-owner";
        when(jobRepository.findById("job-1")).thenReturn(job);
        when(jobRepository.deleteById("job-1")).thenReturn(true);

        ResponseEntity<?> response = controller.deleteJob("job-1", auth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(jobRepository).deleteById("job-1");
    }

    @Test
    void search_delegatesToRepository() {
        when(jobRepository.search(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(java.util.List.of());

        controller.search("q", null, null, null, null, null, null);

        verify(jobRepository).search("q", null, null, null, null, null, null);
    }
}
