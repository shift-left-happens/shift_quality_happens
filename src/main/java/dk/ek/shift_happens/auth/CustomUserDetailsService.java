package dk.ek.shift_happens.auth;

import dk.ek.shift_happens.employee.Employee;
import dk.ek.shift_happens.employee.EmployeeRepository;
import dk.ek.shift_happens.userrole.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Bridges our Employee database with Spring Security's authentication system.
 *
 * Spring Security needs a UserDetailsService to load users during authentication.
 * This implementation:
 *   1. Looks up the employee by email (our "username")
 *   2. Fetches their role from the user_role table
 *   3. Returns a Spring Security User object with:
 *      - email as the username
 *      - BCrypt+pepper hash as the password (Spring Security compares this
 *        against the incoming password using our PepperedPasswordEncoder)
 *      - role mapped to a Spring authority (e.g., "ROLE_ADMINISTRATOR")
 *
 * The "ROLE_" prefix is a Spring Security convention — it allows using
 * hasRole("ADMINISTRATOR") in SecurityConfig, which internally checks for "ROLE_ADMINISTRATOR".
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final EmployeeRepository employeeRepository;
    private final UserRoleRepository userRoleRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Find the employee by email — this is our login identifier
        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Employee not found: " + email));

        // Map the numeric role ID to a role name (e.g., 1 → "Administrator")
        String roleName = userRoleRepository.findById(employee.getFkUserRoleId())
                .map(r -> r.getUserRoleName())
                .orElse("Employee");

        // Build a Spring Security User with the employee's credentials and role
        return new User(
                employee.getEmail(),
                employee.getLoginPassword(),    // BCrypt+pepper hash from the database
                List.of(new SimpleGrantedAuthority("ROLE_" + roleName.toUpperCase()))
        );
    }
}
