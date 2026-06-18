package com.sports.auth_server.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    /** Token validity period: 1 hour in milliseconds */
    private static final long TOKEN_VALIDITY_MS = 1000L * 60 * 60;

    @Value("${jwt.secret}")
    private String secret;

    public void validateToken(final String token) {
        log.debug("Validating JWT token");
        // Throws ExpiredJwtException / MalformedJwtException / SignatureException on failure
        Jwts.parserBuilder()
            .setSigningKey(getSignKey())
            .build()
            .parseClaimsJws(token);
        log.debug("JWT token is valid");
    }

    
//    1. Create header
//    2. Create payload
//    3. Base64 encode both
//    4. Combine them
//    5. Sign using secret key
//    6. Generate final token
    
    
    public String generateToken(String userName, String role) {
        log.debug("Creating JWT token for subject='{}' with role='{}'", userName, role);

        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);   // 🔥 ADD ROLE TO TOKEN

        return createToken(claims, userName);
    }

    private String createToken(Map<String, Object> claims, String subject) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + TOKEN_VALIDITY_MS))
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }


//     Decodes the Base64 secret from properties and constructs an HMAC signing key.

    private Key getSignKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
    
    public String extractRole(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("role", String.class);
    }
}
