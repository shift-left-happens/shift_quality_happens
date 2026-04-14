package dk.ek.shift_happens.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Central Spring Security configuration.
 *
 * Design decisions:
 *   - Stateless sessions (no cookies/session IDs) — every request carries a JWT.
 *   - CSRF disabled because we use JWT Bearer tokens (not cookie-based auth).
 *   - Role hierarchy: ADMINISTRATOR > MANAGER > EMPLOYEE
 *
 * Authorization rules:
 *   /auth/**           → public (login endpoint)
 *   /audit-log/**      → ADMINISTRATOR only
 *   POST/PATCH/DELETE  → ADMINISTRATOR or MANAGER
 *   GET (everything)   → any authenticated user
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final CustomUserDetailsService userDetailsService;

    @Value("${security.pepper}")
    private String pepper;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF — we use stateless JWT auth, not cookies
                .csrf(csrf -> csrf.disable())

                // No HTTP sessions — each request is authenticated via JWT
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        // Public: login endpoint (no token needed)
                        .requestMatchers("/auth/**").permitAll()

                        // Audit log: admin only (sensitive operation history)
                        .requestMatchers("/audit-log/**").hasRole("ADMINISTRATOR")

                        // Write operations: admin and manager can create/update/delete
                        .requestMatchers(HttpMethod.POST, "/**").hasAnyRole("ADMINISTRATOR", "MANAGER")
                        .requestMatchers(HttpMethod.PATCH, "/**").hasAnyRole("ADMINISTRATOR", "MANAGER")
                        .requestMatchers(HttpMethod.PUT, "/**").hasAnyRole("ADMINISTRATOR", "MANAGER")
                        .requestMatchers(HttpMethod.DELETE, "/**").hasAnyRole("ADMINISTRATOR", "MANAGER")

                        // All other requests (GETs): any authenticated user
                        .anyRequest().authenticated()
                )

                // Use our custom auth provider (BCrypt + pepper verification)
                .authenticationProvider(authenticationProvider())

                // Insert JWT filter before Spring's default username/password filter
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * DaoAuthenticationProvider: loads user via CustomUserDetailsService,
     * then verifies password using our PepperedPasswordEncoder.
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Password encoder bean using pepper + BCrypt.
     * See PepperedPasswordEncoder for the full salt/pepper/BCrypt explanation.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new PepperedPasswordEncoder(pepper);
    }
}
