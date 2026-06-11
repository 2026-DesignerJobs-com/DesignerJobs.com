package at.ac.fhcampuswien.application;

import at.ac.fhcampuswien.job.Job;
import at.ac.fhcampuswien.job.JobRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationControllerTest {

    @Mock JobApplicationRepository repository;
    @Mock JobRepository jobRepository;
    @Mock Authentication auth;

    @InjectMocks ApplicationController controller;

    private void authAs(String userId, String role) {
        when(auth.getName()).thenReturn(userId);
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role));
        doReturn(authorities).when(auth).getAuthorities();
    }

    // ---- apply ----

    @Test
    void apply_rejectsUnauthenticated_with401() {
        ResponseEntity<?> response = controller.apply("job-1", new JobApplication(), null);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void apply_rejectsNonDesigner_with403() {
        authAs("client-1", "ROLE_CLIENT");

        ResponseEntity<?> response = controller.apply("job-1", new JobApplication(), auth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(repository, never()).create(anyString(), anyString(), any());
    }

    @Test
    void apply_createsApplication_forDesigner() {
        authAs("designer-1", "ROLE_DESIGNER");
        JobApplication application = new JobApplication();
        application.coverLetter = "hire me";
        JobApplication created = new JobApplication();
        created.id = "app-1";
        when(repository.create("job-1", "designer-1", "hire me")).thenReturn(created);

        ResponseEntity<?> response = controller.apply("job-1", application, auth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(repository).create("job-1", "designer-1", "hire me");
    }

    // ---- listApplications ----

    @Test
    void listApplications_returns404_whenJobMissing() {
        when(auth.getName()).thenReturn("client-1");
        when(jobRepository.findById("missing")).thenReturn(null);

        ResponseEntity<?> response = controller.listApplications("missing", auth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void listApplications_rejectsNonOwner_with403() {
        when(auth.getName()).thenReturn("not-the-owner");
        Job job = new Job();
        job.id = "job-1";
        job.clientId = "the-owner";
        when(jobRepository.findById("job-1")).thenReturn(job);

        ResponseEntity<?> response = controller.listApplications("job-1", auth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(repository, never()).findByJobId(anyString());
    }

    @Test
    void listApplications_allowsJobOwner() {
        when(auth.getName()).thenReturn("the-owner");
        Job job = new Job();
        job.id = "job-1";
        job.clientId = "the-owner";
        when(jobRepository.findById("job-1")).thenReturn(job);
        when(repository.findByJobId("job-1")).thenReturn(List.of(new JobApplication()));

        ResponseEntity<?> response = controller.listApplications("job-1", auth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // ---- updateStatus ----

    @Test
    void updateStatus_rejectsInvalidStatus_with400() {
        when(auth.getName()).thenReturn("client-1");
        JobApplication app = new JobApplication();
        app.status = "PENDING";
        when(repository.findById("app-1")).thenReturn(app);

        ResponseEntity<?> response =
                controller.updateStatus("app-1", Map.of("status", "BANANA"), auth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void updateStatus_rejectsNonPendingApplication_with400() {
        when(auth.getName()).thenReturn("client-1");
        JobApplication app = new JobApplication();
        app.status = "ACCEPTED";
        when(repository.findById("app-1")).thenReturn(app);

        ResponseEntity<?> response =
                controller.updateStatus("app-1", Map.of("status", "REJECTED"), auth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void updateStatus_acceptsPendingApplication() {
        when(auth.getName()).thenReturn("client-1");
        JobApplication app = new JobApplication();
        app.id = "app-1";
        app.status = "PENDING";
        when(repository.findById("app-1")).thenReturn(app);
        JobApplication updated = new JobApplication();
        updated.status = "ACCEPTED";
        when(repository.updateStatus("app-1", "ACCEPTED")).thenReturn(updated);

        ResponseEntity<?> response =
                controller.updateStatus("app-1", Map.of("status", "ACCEPTED"), auth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void updateStatus_returns404_whenApplicationMissing() {
        when(auth.getName()).thenReturn("client-1");
        when(repository.findById("missing")).thenReturn(null);

        ResponseEntity<?> response =
                controller.updateStatus("missing", Map.of("status", "ACCEPTED"), auth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ---- hire ----

    @Test
    void hire_rejectsNonAcceptedApplication_with400() {
        when(auth.getName()).thenReturn("client-1");
        JobApplication app = new JobApplication();
        app.status = "PENDING";
        when(repository.findById("app-1")).thenReturn(app);

        ResponseEntity<?> response = controller.hire("app-1", auth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(repository, never()).updateStatus(anyString(), anyString());
    }

    @Test
    void hire_promotesAcceptedApplicationToHired() {
        when(auth.getName()).thenReturn("client-1");
        JobApplication app = new JobApplication();
        app.id = "app-1";
        app.status = "ACCEPTED";
        when(repository.findById("app-1")).thenReturn(app);
        JobApplication hired = new JobApplication();
        hired.status = "HIRED";
        when(repository.updateStatus("app-1", "HIRED")).thenReturn(hired);

        ResponseEntity<?> response = controller.hire("app-1", auth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(repository).updateStatus("app-1", "HIRED");
    }
}
