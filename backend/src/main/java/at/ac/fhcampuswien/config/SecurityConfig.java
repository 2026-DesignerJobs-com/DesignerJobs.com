package at.ac.fhcampuswien.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .headers(headers -> headers
                        // ADD THIS LINE: It allows the iframe shell to load your inner pages
                        .frameOptions(frameOptions -> frameOptions.sameOrigin())
                )
            .authorizeHttpRequests(auth -> auth
                // Phase 1: all routes public (stubs return 501); tighten in Phase 2
                .anyRequest().permitAll()
            )
            .httpBasic(httpBasic -> {})
            .csrf(csrf -> csrf.disable());
        return http.build();
    }
}