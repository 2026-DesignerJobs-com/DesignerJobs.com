package at.ac.fhcampuswien.auth;

public class AuthRequest {
    public String email;
    public String password;
    public String role;  // CLIENT or DESIGNER (register only)
}