package at.ac.fhcampuswien.account;

import jakarta.validation.constraints.Size;

// @Size caps mirror the users-table VARCHAR limits, so over-long input is a 400, not an H2 500 (B24).
public class ProfileUpdateRequest {
    @Size(max = 255) public String fullName;
    @Size(max = 255) public String designType;
    @Size(max = 2000) public String bio;
    @Size(max = 255) public String country;
    @Size(max = 255) public String city;
    @Size(max = 50) public String availability;
    @Size(max = 1000) public String skills;
    @Size(max = 50) public String portfolioVisibility;
    @Size(max = 500) public String portfolioUrl;
    @Size(max = 255) public String twitter;
    @Size(max = 500) public String linkedin;
    @Size(max = 255) public String instagram;

    // Boxed so an omitted field is null (not 0) and a partial update doesn't clobber the stored rate.
    public Integer hourlyMin;
    public Integer hourlyMax;
    public Integer projectMin;
}