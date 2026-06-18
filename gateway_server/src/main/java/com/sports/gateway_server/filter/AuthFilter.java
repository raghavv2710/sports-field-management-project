package com.sports.gateway_server.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.Base64;


@Component
public class AuthFilter extends AbstractGatewayFilterFactory<AuthFilter.Config> {

    private static final Logger log = LoggerFactory.getLogger(AuthFilter.class);

    @Autowired
    private WebClient.Builder webClientBuilder;

    public AuthFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            HttpHeaders headers = exchange.getRequest().getHeaders();
            String path = exchange.getRequest().getPath().toString();
            String method = exchange.getRequest().getMethod().name();

            // Step 1: Check Authorization header presence
            if (!headers.containsKey(HttpHeaders.AUTHORIZATION)) {
                log.warn("Rejected {}: missing Authorization header", path);
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            String authHeader = headers.getFirst(HttpHeaders.AUTHORIZATION);

            // Step 2: Check Bearer prefix
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("Rejected {}: Authorization header must start with 'Bearer '", path);
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            // Step 3: Extract token
            String token = authHeader.substring(7);
            log.debug("Validating JWT for {} {}", method, path);

            // Step 4: Call auth-server to validate token (reactive/non-blocking)
            return webClientBuilder.build()
                    .get()
                    .uri("http://AUTH-SERVER/auth/validate?token=" + token)
                    .retrieve()
                    .toBodilessEntity()
                    .flatMap(response -> {

                        // Step 5: Extract role from JWT payload
                        String role = extractRole(token);
                        log.debug("Role from token: {} for {} {}", role, method, path);

                        // Step 6: RBAC — field management requires ROLE_ADMIN
                        // FIX: Added "PUT" — users could update fields without being admin
                        boolean isFieldModification = path.contains("/api/fields") &&
                                (method.equals("POST") ||
                                 method.equals("PUT") ||
                                 method.equals("DELETE"));

                        if (isFieldModification && !role.equals("ROLE_ADMIN")) {
                            log.warn("Access denied: {} tried {} on {} — requires ROLE_ADMIN",
                                    role, method, path);
                            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                            return exchange.getResponse().setComplete();
                        }

                        log.debug("Access granted: {} {} for role {}", method, path, role);
                        return chain.filter(exchange);
                    })
                    .onErrorResume(ex -> {
                        // Token invalid, expired, or auth-server unreachable
                        log.warn("Token validation failed for {} {}: {}", method, path, ex.getMessage());
                        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                        return exchange.getResponse().setComplete();
                    });
        };
    }


    private String extractRole(String token) {
        try {
            // Decode the payload (middle part of the JWT)
            String payload = new String(Base64.getDecoder().decode(token.split("\\.")[1]));
            if (payload.contains("ROLE_ADMIN")) {
                return "ROLE_ADMIN";
            }
        } catch (Exception e) {
            log.error("Error extracting role from JWT payload: {}", e.getMessage());
        }
        return "ROLE_USER";
    }


    public static class Config {
        // No configuration properties required
    }
}
