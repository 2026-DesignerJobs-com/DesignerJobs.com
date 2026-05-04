package at.ac.fhcampuswien;

/**
 * a job listing.
 */

public class Job {
    public String id;
    public String clientId;
    public String title;
    public String description;
    public String category;    // webdesign, graphic design, interior design
    public String designType;  // logo, branding, ui/ux, web, illustration, interior, motion
    public String location;
    public String budget;      // small, medium, big
    public String workMode;    // remote, on site, hybrid
    public String deadline;    // ISO 8601 date
    public String tags;        // comma-separated
    public String createdAt;   // ISO 8601
}
