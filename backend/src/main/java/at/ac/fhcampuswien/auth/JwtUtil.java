package at.ac.fhcampuswien.auth;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

/**
 * Utility class for creating simple JWT tokens.
 *
 * The JWT contains:
 * - userId as subject
 * - role as user role
 * - expiration time
 *
 * This is a simple educational implementation for the project.
 */
public class JwtUtil {

    // Secret key used to sign the token.
    // Later this should be moved to application.properties or an environment variable.
    private static final String SECRET = "designer-jobs-secret-key-for-development";

    // Token validity: 2 hours.
    private static final long EXPIRATION_SECONDS = 2 * 60 * 60;

    /**
     * Creates a signed JWT token for a logged-in user.
     */
    public static String generateToken(String userId, String role) {
        String headerJson = """
                {"alg":"HS256","typ":"JWT"}
                """.trim();

        long expiresAt = Instant.now().getEpochSecond() + EXPIRATION_SECONDS;

        String payloadJson = """
                {"sub":"%s","role":"%s","exp":%d}
                """.formatted(userId, role, expiresAt).trim();

        String header = base64UrlEncode(headerJson);
        String payload = base64UrlEncode(payloadJson);

        String unsignedToken = header + "." + payload;
        String signature = sign(unsignedToken);

        return unsignedToken + "." + signature;
    }

    /**
     * Encodes JSON into Base64 URL format.
     */
    private static String base64UrlEncode(String value) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Creates the JWT signature with HMAC-SHA256.
     */
    private static String sign(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");

            SecretKeySpec secretKey = new SecretKeySpec(
                    SECRET.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );

            mac.init(secretKey);

            byte[] signatureBytes = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));

            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(signatureBytes);

        } catch (Exception e) {
            throw new RuntimeException("Failed to create JWT signature", e);
        }
    }
}