package at.ac.fhcampuswien.account;

public class UserModel {
    // Authentication and core identity
    public String id;
    public String email;
    public String passwordHash;
    public String role;       // CLIENT or DESIGNER
    public String createdAt;

    // Basic profile information
    public String fullName;
    public String designType;
    public String bio;
    public String skills;

    // Location and availability
    public String country;
    public String city;
    public String availability;

    // Rates
    public int hourlyMin;
    public int hourlyMax;
    public int projectMin;

    // Portfolio and social links
    public String portfolioVisibility;
    public String portfolioUrl;
    public String twitter;
    public String linkedin;
    public String instagram;
}