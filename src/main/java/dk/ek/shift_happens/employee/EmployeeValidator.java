package dk.ek.shift_happens.employee;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Validates Employee fields per the blackbox spec
 * (Software Quality exam - Shift Happens (1).docx).
 *
 * Field rules:
 *   - Name (first + last): letters only (Unicode), length 1..100. Single internal
 *     space allowed (docx §"First Name & last name" case 5: "Jensen Jens" is Valid).
 *   - Password: length 8..64, must contain >=1 lowercase, >=1 uppercase, >=1 digit.
 *   - Email: length 5..320, exactly one '@', non-empty local part with no spaces,
 *     domain has >=1 letter + '.' + TLD of >=2 letters. Uniqueness checked at call site.
 *   - Birth date: required, not in the future, age 16..100 against the injected Clock.
 */
@Component
@RequiredArgsConstructor
public class EmployeeValidator {

    public static final int NAME_MIN = 1;
    public static final int NAME_MAX = 100;
    public static final int PASSWORD_MIN = 8;
    public static final int PASSWORD_MAX = 64;
    public static final int EMAIL_MIN = 5;
    public static final int EMAIL_MAX = 320;
    public static final int AGE_MIN = 16;
    public static final int AGE_MAX = 100;

    private final Clock clock;

    public void validateForCreate(Employee employee, boolean emailAlreadyTaken) {
        validateName(employee.getFirstName(), "firstName");
        validateName(employee.getLastName(), "lastName");
        validatePassword(employee.getLoginPassword());
        validateEmail(employee.getEmail(), emailAlreadyTaken);
        validateBirthDate(employee.getBirthDate());
    }

    public void validateName(String name, String field) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be empty");
        }
        if (name.length() < NAME_MIN || name.length() > NAME_MAX) {
            throw new IllegalArgumentException(field + " length must be between " + NAME_MIN + " and " + NAME_MAX);
        }
        // Letters only, at most one internal single-space separator (docx case 5).
        // Allowed: "Jensen", "Jensen Jens". Disallowed: digits, symbols, leading/trailing space,
        // multiple consecutive spaces.
        if (!name.matches("\\p{L}+( \\p{L}+)?")) {
            throw new IllegalArgumentException(field + " must contain letters only");
        }
    }

    public void validatePassword(String password) {
        if (password == null) {
            throw new IllegalArgumentException("password is required");
        }
        if (password.length() < PASSWORD_MIN || password.length() > PASSWORD_MAX) {
            throw new IllegalArgumentException(
                    "password length must be between " + PASSWORD_MIN + " and " + PASSWORD_MAX);
        }
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        if (!hasLower) throw new IllegalArgumentException("password must contain a lowercase letter");
        if (!hasUpper) throw new IllegalArgumentException("password must contain an uppercase letter");
        if (!hasDigit) throw new IllegalArgumentException("password must contain a digit");
    }

    public void validateEmail(String email, boolean alreadyTaken) {
        if (email == null) {
            throw new IllegalArgumentException("email is required");
        }
        if (email.length() < EMAIL_MIN || email.length() > EMAIL_MAX) {
            throw new IllegalArgumentException("email length must be between " + EMAIL_MIN + " and " + EMAIL_MAX);
        }
        int atIndex = email.indexOf('@');
        if (atIndex < 0 || email.indexOf('@', atIndex + 1) >= 0) {
            throw new IllegalArgumentException("email must contain exactly one @");
        }
        String local = email.substring(0, atIndex);
        String domain = email.substring(atIndex + 1);
        if (local.isEmpty() || local.contains(" ")) {
            throw new IllegalArgumentException("email local part is invalid");
        }
        // Domain: at least one letter, then '.', then a TLD of >=2 letters.
        if (!domain.matches("[\\p{L}\\p{N}._-]*\\p{L}[\\p{L}\\p{N}._-]*\\.\\p{L}{2,}")) {
            throw new IllegalArgumentException("email domain is invalid");
        }
        if (alreadyTaken) {
            throw new IllegalArgumentException("email is already in use");
        }
    }

    public void validateBirthDate(LocalDate birthDate) {
        if (birthDate == null) {
            throw new IllegalArgumentException("birthDate is required");
        }
        LocalDate today = LocalDate.now(clock);
        if (birthDate.isAfter(today)) {
            throw new IllegalArgumentException("birthDate must not be in the future");
        }
        int age = Period.between(birthDate, today).getYears();
        if (age < AGE_MIN) {
            throw new IllegalArgumentException("employee must be at least " + AGE_MIN + " years old");
        }
        if (age > AGE_MAX) {
            throw new IllegalArgumentException("employee must not be older than " + AGE_MAX + " years");
        }
    }
}
