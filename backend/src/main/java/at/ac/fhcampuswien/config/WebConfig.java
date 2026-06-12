package at.ac.fhcampuswien.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.frontend.path:../frontend/design3/}")
    private String frontendPath;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(
                        "http://localhost:8080",      // Spring Boot Server
                        "http://127.0.0.1:8080",
                        "http://localhost:63342",     // IntelliJ Built-in Server
                        "http://127.0.0.1:63342"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        registry.addResourceHandler("/admin/**")
                .addResourceLocations("file:" + frontendPath + "../admin/");

        registry.addResourceHandler("/**")
                .addResourceLocations("file:" + frontendPath);
    }
}
