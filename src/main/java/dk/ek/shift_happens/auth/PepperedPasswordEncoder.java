package dk.ek.shift_happens.auth;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Custom PasswordEncoder that adds a "pepper" to passwords before BCrypt hashing.
 *
 * Password security uses three layers:
 *   1. PEPPER — a server-side secret prepended to every password before hashing.
 *              Stored in application config (NOT in the database). Even if the DB
 *              is fully compromised, attackers can't crack hashes without the pepper.
 *   2. SALT  — a random value unique to each password hash. BCrypt generates this
 *              automatically and stores it inside the hash string itself (the $2b$10$...
 *              prefix contains the salt). This prevents rainbow table attacks and ensures
 *              two identical passwords produce different hashes.
 *   3. BCRYPT — an adaptive hashing algorithm with a configurable cost factor (here: 10).
 *              It is intentionally slow, making brute-force attacks expensive.
 *
 * Flow:
 *   encode("password123")  →  BCrypt.hash(pepper + "password123" + auto-generated salt)
 *   matches("password123") →  BCrypt.verify(pepper + "password123", storedHash)
 */
public class PepperedPasswordEncoder implements PasswordEncoder {

    private final String pepper;
    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

    public PepperedPasswordEncoder(String pepper) {
        this.pepper = pepper;
    }

    /**
     * Hash a raw password: prepend the pepper, then BCrypt with auto-generated salt.
     */
    @Override
    public String encode(CharSequence rawPassword) {
        return bcrypt.encode(pepper + rawPassword);
    }

    /**
     * Verify a raw password against a stored BCrypt hash.
     * The same pepper is prepended before comparison.
     */
    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        return bcrypt.matches(pepper + rawPassword, encodedPassword);
    }
}
