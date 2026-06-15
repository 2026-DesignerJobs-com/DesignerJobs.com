package at.ac.fhcampuswien.infrastructure.config;

import at.ac.fhcampuswien.account.UserModel;
import at.ac.fhcampuswien.account.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Component
public class AdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        String adminEmail = "real-admin@designerjobs.com".trim().toLowerCase(Locale.ROOT);

        boolean adminExists = userRepository.findAll().stream()
                .anyMatch(user -> adminEmail.equalsIgnoreCase(user.email));

        if (!adminExists) {
            UserModel admin = new UserModel();
            admin.id = UUID.randomUUID().toString();
            admin.fullName = "Plattform Admin";
            admin.email = adminEmail;

            admin.passwordHash = passwordEncoder.encode("AdminPasswort123!");

            // Direkt als ADMIN in die Datenbank setzen
            admin.role = "ADMIN";
            admin.createdAt = Instant.now().toString();

            // Manuelle Absicherung aller Profilfelder gegen SQL-NULL
            admin.designType = "";
            admin.bio = "";
            admin.skills = "";
            admin.country = "";
            admin.city = "";
            admin.availability = "";
            admin.hourlyMin = 0;
            admin.hourlyMax = 0;
            admin.projectMin = 0;
            admin.portfolioVisibility = "";
            admin.portfolioUrl = "";
            admin.twitter = "";
            admin.linkedin = "";
            admin.instagram = "";

            userRepository.save(admin);
            System.out.println(">>> [SUCCESS] Admin-Account wurde in der DB angelegt: " + adminEmail);
        } else {
            System.out.println(">>> [INFO] Admin-Account existiert bereits in der DB.");
        }
    }
}