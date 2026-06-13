package at.ac.fhcampuswien.account;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import at.ac.fhcampuswien.infrastructure.session.JwtService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock UserRepository userRepository;
    @Mock JwtService jwtService;
    @Mock PasswordEncoder passwordEncoder;

    @InjectMocks AuthController controller;

    private AuthRequest registerRequest(String email, String role) {
        AuthRequest req = new AuthRequest();
        req.fullName = "Jane Doe";
        req.email = email;
        req.password = "secret123";
        req.role = role;
        return req;
    }

    // ---- register ----

    @Test
    void register_rejectsMissingFields_with400() {
        ResponseEntity<?> response = controller.register(new AuthRequest());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void register_rejectsUnknownRole_with400() {
        ResponseEntity<?> response = controller.register(registerRequest("a@test.com", "ADMIN"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void register_rejectsDuplicateEmail_with409() {
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        ResponseEntity<?> response = controller.register(registerRequest("a@test.com", "CLIENT"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void register_normalizesRoleToUpperCase_andStoresLowercaseEmail() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.issue(anyString(), anyString())).thenReturn("jwt-token");

        ResponseEntity<?> response = controller.register(registerRequest("Mixed@Test.com", "designer"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ArgumentCaptor<UserModel> captor = ArgumentCaptor.forClass(UserModel.class);
        verify(userRepository).save(captor.capture());
        UserModel saved = captor.getValue();
        assertThat(saved.role).isEqualTo("DESIGNER");
        assertThat(saved.email).isEqualTo("mixed@test.com");
        assertThat(saved.passwordHash).isNotEqualTo("secret123"); // encoded, not raw
    }

    @Test
    void register_returnsTokenAndIdentity() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.issue(anyString(), anyString())).thenReturn("jwt-token");

        ResponseEntity<?> response = controller.register(registerRequest("a@test.com", "CLIENT"));

        AuthResponse body = (AuthResponse) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.token).isEqualTo("jwt-token");
        assertThat(body.role).isEqualTo("CLIENT");
        assertThat(body.userId).isNotBlank();
    }

    // ---- login ----

    @Test
    void login_rejectsMissingFields_with400() {
        ResponseEntity<?> response = controller.login(new AuthRequest());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void login_rejectsUnknownUser_with401() {
        when(userRepository.findByEmail(anyString())).thenReturn(null);

        AuthRequest req = new AuthRequest();
        req.email = "a@test.com";
        req.password = "secret123";

        ResponseEntity<?> response = controller.login(req);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void login_rejectsWrongPassword_with401() {
        UserModel user = new UserModel();
        user.id = "u1";
        user.email = "a@test.com";
        user.passwordHash = "hashed";
        user.role = "CLIENT";
        when(userRepository.findByEmail("a@test.com")).thenReturn(user);
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        AuthRequest req = new AuthRequest();
        req.email = "a@test.com";
        req.password = "wrong";

        ResponseEntity<?> response = controller.login(req);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void login_succeedsWithCorrectCredentials() {
        UserModel user = new UserModel();
        user.id = "u1";
        user.email = "a@test.com";
        user.passwordHash = "hashed";
        user.role = "CLIENT";
        when(userRepository.findByEmail("a@test.com")).thenReturn(user);
        when(passwordEncoder.matches("secret123", "hashed")).thenReturn(true);
        when(jwtService.issue("u1", "CLIENT")).thenReturn("jwt-token");

        AuthRequest req = new AuthRequest();
        req.email = "a@test.com";
        req.password = "secret123";

        ResponseEntity<?> response = controller.login(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((AuthResponse) response.getBody()).token).isEqualTo("jwt-token");
    }
}
