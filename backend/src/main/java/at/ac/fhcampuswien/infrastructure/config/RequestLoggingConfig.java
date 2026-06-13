package at.ac.fhcampuswien.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

/**
 * Logs every incoming HTTP request (method, URL, query string, client info,
 * headers and body payload) for debugging.
 *
 * <p>The filter logs at DEBUG on the logger
 * {@code org.springframework.web.filter.CommonsRequestLoggingFilter}, which is
 * covered by the {@code logging.level.org.springframework.web=DEBUG} setting in
 * {@code application.properties}. Lower that to INFO to silence request logs
 * without touching this class.
 */
@Configuration
public class RequestLoggingConfig {

    @Bean
    public CommonsRequestLoggingFilter requestLoggingFilter() {
        CommonsRequestLoggingFilter filter = new CommonsRequestLoggingFilter();
        filter.setIncludeClientInfo(true);
        filter.setIncludeQueryString(true);
        filter.setIncludeHeaders(true);
        // Never log credentials: drop the Authorization (bearer JWT) and Cookie headers,
        // and don't log request bodies — login/register payloads carry plaintext passwords (H6).
        filter.setHeaderPredicate(headerName ->
                !headerName.equalsIgnoreCase("authorization") && !headerName.equalsIgnoreCase("cookie"));
        filter.setIncludePayload(false);
        filter.setAfterMessagePrefix("HTTP REQUEST: ");
        return filter;
    }
}
