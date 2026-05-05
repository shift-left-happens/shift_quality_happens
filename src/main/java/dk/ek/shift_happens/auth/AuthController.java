package dk.ek.shift_happens.auth;

import dk.ek.shift_happens.employee.Employee;
import dk.ek.shift_happens.employee.EmployeeRepository;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication endpoint — the only public endpoint in the API.
 *
 * Login flow:
 *   1. Client sends POST /auth/login with { email, password }
 *   2. AuthenticationManager delegates to CustomUserDetailsService to load the user
 *   3. PepperedPasswordEncoder verifies: BCrypt(pepper + inputPassword) == storedHash
 *   4. If valid, we generate a JWT containing the user's identity and role
 *   5. Client stores the JWT and sends it in all subsequent requests as:
 *      "Authorization: Bearer <token>"
 *
 * Logout:
 *   JWT is stateless — the client simply discards the token.
 *   No server-side session to invalidate.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final EmployeeRepository employeeRepository;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        // Attempt authentication — Spring Security handles password verification
        // using PepperedPasswordEncoder (pepper + BCrypt + salt)
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        } catch (BadCredentialsException e) {
            // Generic error message — don't reveal whether email or password was wrong
            return ResponseEntity.status(401).body(Map.of("error", "Invalid email or password"));
        }

        // Authentication succeeded — load employee details for the JWT payload
        Employee employee = employeeRepository.findByEmail(request.getEmail()).orElseThrow();

        String roleName =
                employee.getUserRole() != null ? employee.getUserRole().getRoleName() : "Employee";

        // Generate JWT with user identity embedded in the claims
        String token = jwtService.generateToken(employee.getEmail(), employee.getEmployeeId(), roleName);

        return ResponseEntity.ok(new LoginResponse(
                token,
                employee.getEmployeeId(),
                employee.getEmployeeNumber(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getEmail(),
                roleName));
    }
}
