package at.ac.fhcampuswien.infrastructure.session;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class JwtService {

    private final JwtEncoder encoder;
    private final long expiryMillis;

    public JwtService(JwtEncoder encoder,
                      @Value("${app.jwt.expiry-millis}") long expiryMillis) {
        this.encoder = encoder;
        this.expiryMillis = expiryMillis;
    }

    public String issue(String userId, String role) {
        Instant now = Instant.now();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(userId)
                .claim("role", role)
                .issuedAt(now)
                .expiresAt(now.plus(expiryMillis, ChronoUnit.MILLIS))
                .build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
