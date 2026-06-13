package at.ac.fhcampuswien;

import at.ac.fhcampuswien.infrastructure.Database.DatabaseInitializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = { UserDetailsServiceAutoConfiguration.class })
public class Main {
    public static void main(String[] args) {
        DatabaseInitializer.init();
        SpringApplication.run(Main.class, args);
    }
}