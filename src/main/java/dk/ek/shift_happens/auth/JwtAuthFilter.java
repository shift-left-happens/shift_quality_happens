package dk.ek.shift_happens.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * JWT authentication filter — runs once per request, before Spring's default auth filters.
 *
 * How it works:
 *   1. Reads the "Authorization" header from the incoming HTTP request
 *   2. If the header starts with "Bearer ", extracts the JWT token
 *   3. Validates the token (signature check + expiration check via JwtService)
 *   4. If valid, loads the user from the database and sets the SecurityContext
 *      — this tells Spring Security "this request is authenticated as user X with roles Y"
 *   5. If the header is missing or the token is invalid, the request continues
 *      unauthenticated — Spring Security will then reject it (unless the endpoint is public)
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        // Step 1: Check for Authorization header
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // No token present — continue without authentication
            filterChain.doFilter(request, response);
            return;
        }

        // Step 2: Extract the token (everything after "Bearer ")
        String token = authHeader.substring(7);

        try {
            // Step 3: Extract email from the token's claims
            String email = jwtService.extractEmail(token);

            // Step 4: Only authenticate if we got an email and there's no existing auth
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                // Step 5: Validate token (correct user + not expired)
                if (jwtService.isValid(token, userDetails)) {
                    // Step 6: Create auth token with user's roles and set it in the context
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception ignored) {
            // Invalid/expired/malformed token — request proceeds unauthenticated
            // Spring Security will return 401/403 for protected endpoints
        }

        filterChain.doFilter(request, response);
    }
}
