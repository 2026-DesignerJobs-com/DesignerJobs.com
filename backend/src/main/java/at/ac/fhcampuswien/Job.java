package at.ac.fhcampuswien;

/**
 * a job listing.
 */

public class Job {
    public String id;
    public String title;
    public String description; // "seeking a creative webdesigner to ... for our Berlin office"
    public String category;   // webdesign, graphic design, interior design
    public String location;   // Berlin, Munich, Hamburg, ...
    public String budget;     // small, medium, big
    public String workMode;   // remote, on site, hybrid
    public String createdAt;  // ISO 8601
}
