package at.ac.fhcampuswien.session;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "designer-jobs-development-secret-key-please-change-me-32";

    private final SecretKey key =
            new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");

    private JwtEncoder encoder() {
        OctetSequenceKey jwk = new OctetSequenceKey.Builder(key.getEncoded()).build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(jwk)));
    }

    private JwtDecoder decoder() {
        return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Test
    void issue_producesTokenWithSubjectRoleAndExpiry() {
        JwtService service = new JwtService(encoder(), 7_200_000L);

        String token = service.issue("user-1", "DESIGNER");
        Jwt decoded = decoder().decode(token);

        assertThat(decoded.getSubject()).isEqualTo("user-1");
        assertThat(decoded.getClaimAsString("role")).isEqualTo("DESIGNER");
        assertThat(decoded.getExpiresAt()).isNotNull();
        assertThat(decoded.getIssuedAt()).isNotNull();
    }

    @Test
    void issue_setsExpiryRoughlyExpiryMillisInTheFuture() {
        JwtService service = new JwtService(encoder(), 7_200_000L);

        Jwt decoded = decoder().decode(service.issue("user-1", "CLIENT"));

        long secondsUntilExpiry =
                decoded.getExpiresAt().getEpochSecond() - Instant.now().getEpochSecond();
        // 7_200_000 ms = 7200 s (2 hours); allow generous slack for clock/run time.
        assertThat(secondsUntilExpiry).isBetween(7000L, 7300L);
    }
}
