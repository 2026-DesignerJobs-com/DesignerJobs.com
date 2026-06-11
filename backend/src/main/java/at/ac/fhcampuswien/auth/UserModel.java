package at.ac.fhcampuswien.auth;

public class UserModel {
    // Authentifizierungs- und Basisdaten
    public String id;
    public String email;
    public String passwordHash;
    public String role;       // CLIENT or DESIGNER
    public String createdAt;

    // Profil-Basisinformationen
    public String fullName;
    public String designType;
    public String bio;        // <-- NEU
    public String skills;

    // Standort & Verfügbarkeit
    public String country;    // <-- NEU
    public String city;       // <-- NEU
    public String availability; // <-- NEU

    // Tarife / Rates
    public int hourlyMin;     // <-- NEU
    public int hourlyMax;     // <-- NEU
    public int projectMin;    // <-- NEU

    // Portfolio & Social Links
    public String portfolioVisibility; // <-- NEU
    public String portfolioUrl;        // <-- NEU
    public String twitter;             // <-- NEU
    public String linkedin;            // <-- NEU
    public String instagram;           // <-- NEU
}