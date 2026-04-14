package dk.ek.shift_happens.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * Service for creating and validating JSON Web Tokens (JWT).
 *
 * JWT flow:
 *   1. User logs in with email + password → AuthController calls generateToken()
 *   2. Token is returned to the client (contains email, role, employeeId)
 *   3. Client sends token in every request: "Authorization: Bearer <token>"
 *   4. JwtAuthFilter calls extractEmail() and isValid() to authenticate the request
 *
 * Token structure (3 parts, base64-encoded, separated by dots):
 *   HEADER.PAYLOAD.SIGNATURE
 *   - Header:    algorithm (HS256) + type (JWT)
 *   - Payload:   subject (email), claims (role, employeeId), issued-at, expiration
 *   - Signature: HMAC-SHA256(header + payload, secret key) — proves token wasn't tampered with
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms:86400000}") long expirationMs) {
        // HMAC-SHA key derived from the secret string — used to sign and verify tokens
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /**
     * Generate a signed JWT containing the user's identity and role.
     * The token expires after the configured duration (default: 24 hours).
     */
    public String generateToken(String email, Integer employeeId, String roleName) {
        return Jwts.builder()
                .subject(email)                             // Primary identifier
                .claims(Map.of(
                        "employeeId", employeeId,           // Embedded for quick lookups
                        "role", roleName                    // Used for frontend role checks
                ))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key)                              // HMAC-SHA256 signature
                .compact();
    }

    /** Extract the email (subject) from a token. */
    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    /** Validate: token belongs to this user and hasn't expired. */
    public boolean isValid(String token, UserDetails userDetails) {
        String email = extractEmail(token);
        return email.equals(userDetails.getUsername()) && !isExpired(token);
    }

    private boolean isExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }

    /** Parse and verify the token signature, returning all claims. */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
