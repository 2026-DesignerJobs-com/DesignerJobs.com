package at.ac.fhcampuswien.contract;

public class Contract {
    public String id;
    public String applicationId;
    public String clientId;
    public String designerId;
    public String terms;       // auto-generated standard freelance contract text
    public String status;      // DRAFT, SIGNED_CLIENT, SIGNED_DESIGNER, ACTIVE, CANCELLED
    public String createdAt;
}