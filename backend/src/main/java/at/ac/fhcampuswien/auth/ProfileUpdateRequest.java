package at.ac.fhcampuswien.auth;

public class ProfileUpdateRequest {
    public String fullName;
    public String designType;
    public String bio;
    public String country;
    public String city;
    public String availability;
    public String skills;
    public String portfolioVisibility;
    public String portfolioUrl;
    public String twitter;
    public String linkedin;
    public String instagram;

    // Boxed so an omitted field is null (not 0) and a partial update doesn't clobber the stored rate.
    public Integer hourlyMin;
    public Integer hourlyMax;
    public Integer projectMin;
}