package at.ac.fhcampuswien;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication // enables auto-configuration and component scanning
public class Main {
    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    } // starts the Spring Boot application, which will listen for HTTP requests and route themo to controllers
}
