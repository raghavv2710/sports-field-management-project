package com.sports.auth_server.config;

import com.sports.auth_server.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT authentication filter for auth-server's own incoming requests.
 *
 * Why this is needed:
 *   /auth/register/admin has @PreAuthorize("hasRole('ADMIN')").
 *   For that annotation to work, Spring Security must know the caller's role.
 *   That means we must read the JWT from the Authorization header, validate it,
 *   extract the role, and set it in the SecurityContext — before the controller runs.
 *
 * Flow:
 *   Request arrives → this filter runs → reads Bearer token → validates →
 *   extracts role → sets SecurityContext → controller runs → @PreAuthorize checks role
 *
 * If no token is present, the filter does nothing and lets the request
 * continue — permitAll() paths (register, token, validate) work unchanged.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    @Autowired
    private JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // No Authorization header — let the request continue as anonymous
        // (public endpoints like /auth/register and /auth/token still work)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7); // strip "Bearer " prefix

        try {
            // Validate signature and expiry — throws exception if invalid
            jwtService.validateToken(token);

            // Extract the role claim from the JWT payload
            String role = jwtService.extractRole(token); // e.g. "ROLE_ADMIN"

            // Build a Spring Security authentication object with the role
            // This is what @PreAuthorize("hasRole('ADMIN')") reads
            UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                    null,                                       // principal (not needed)
                    null,                                       // credentials (not needed)
                    List.of(new SimpleGrantedAuthority(role))   // authorities → role
                );

            // Set in the security context — now @PreAuthorize can see the role
            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("JWT authenticated successfully with role='{}'", role);

        } catch (Exception e) {
            // Invalid or expired token — clear context and continue
            // The request will hit the endpoint as anonymous
            // @PreAuthorize will then deny it with 403
            log.warn("JWT validation failed in JwtAuthFilter: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}