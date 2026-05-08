package at.ac.fhcampuswien.auth;

public class UserModel {
    public String id;
    public String email;
    public String passwordHash;
    public String role;       // CLIENT or DESIGNER
    public String createdAt;
}