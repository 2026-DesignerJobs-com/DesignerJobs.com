package at.ac.fhcampuswien.application;

// Thrown when an INSERT hits the UNIQUE (job_id, designer_id) constraint — a duplicate apply.
// Lets the controller answer 409 instead of leaking a 500 on the create-race.
public class DuplicateApplicationException extends RuntimeException {
    public DuplicateApplicationException() {
        super("designer has already applied to this job");
    }
}
