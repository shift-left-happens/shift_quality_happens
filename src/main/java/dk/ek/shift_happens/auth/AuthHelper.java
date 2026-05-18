package dk.ek.shift_happens.auth;

import dk.ek.shift_happens.employee.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Shared helpers for controllers that need per-row authorization on top of
 * the global rules in SecurityConfig. Keeps role checks and email→employeeId
 * resolution in one place so controllers don't duplicate them.
 */
@Component
@RequiredArgsConstructor
public class AuthHelper {

    private final EmployeeRepository employeeRepository;

    public boolean isEmployee(Authentication auth) {
        return hasRole(auth, "ROLE_EMPLOYEE");
    }

    public Integer currentEmployeeId(Authentication auth) {
        return getEmployeeIdFromEmail(auth.getName());
    }

    public Integer getEmployeeIdFromEmail(String email) {
        return employeeRepository
                .findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Authenticated user has no matching employee record"))
                .getEmployeeId();
    }

    public ResponseStatusException forbidden() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
    }

    private boolean hasRole(Authentication auth, String role) {
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(role));
    }
}
