package at.ac.fhcampuswien.moderation;

public class Report {
    public String id;
    public String reporterId;
    public String targetType;  // JOB, MESSAGE, USER
    public String targetId;
    public String reason;
    public String status;      // OPEN, RESOLVED, DISMISSED
    public String createdAt;
}