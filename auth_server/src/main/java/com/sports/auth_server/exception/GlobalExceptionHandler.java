package com.sports.auth_server.exception;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

//HTTP status mapping:
//	   BadCredentialsException             → 401 Unauthorized  (wrong password)
//	   UsernameNotFoundException           → 404 Not Found     (user doesn't exist)
//	   ExpiredJwtException                 → 401 Unauthorized  (token expired)
//     MalformedJwtException               → 401 Unauthorized  (token tampered)
//     SignatureException                  → 401 Unauthorized  (wrong secret)
//     MethodArgumentNotValidException     → 400 Bad Request   (validation failure)
//     RuntimeException                    → 500 Internal Server Error
//
//     All responses share this JSON structure:
//     { time stamp, status, error, message }
   
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

//    Wrong username or password during /auth/token 
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException ex) {
        log.warn("Bad credentials attempt: {}", ex.getMessage());
        return buildError(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid username or password");
    }

//    Username not found in database during /auth/token 
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUserNotFound(UsernameNotFoundException ex) {
        log.warn("User not found: {}", ex.getMessage());
        return buildError(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", ex.getMessage());
    }

//    JWT token has passed its expiry time 
    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<Map<String, Object>> handleExpiredJwt(ExpiredJwtException ex) {
        log.warn("Expired JWT token received");
        return buildError(HttpStatus.UNAUTHORIZED, "TOKEN_EXPIRED",
                "Token has expired. Please log in again to get a new token.");
    }

//    JWT token is structurally invalid or has been tampered with 
    @ExceptionHandler({MalformedJwtException.class, SignatureException.class})
    public ResponseEntity<Map<String, Object>> handleInvalidJwt(Exception ex) {
        log.warn("Invalid JWT token: {}", ex.getMessage());
        return buildError(HttpStatus.UNAUTHORIZED, "TOKEN_INVALID", "Invalid token signature or format.");
    }

//    @Valid annotation validation failures (blank name, short password, bad email) 
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("Validation failed: {}", message);
        return buildError(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }

//    Catch-all for unexpected errors 
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntime(RuntimeException ex) {
        log.error("Unhandled exception in auth-server: {}", ex.getMessage(), ex);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR",
                ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred");
    }

    private ResponseEntity<Map<String, Object>> buildError(HttpStatus status, String code, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", code);
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
    
//    Thrown by Spring Security when isEnabled() returns false on CustomUserDetails (when admin deactivates a user)

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<Map<String, Object>> handleDisabled(DisabledException ex) {
        log.warn("Login attempt by deactivated account");
        return buildError(HttpStatus.FORBIDDEN, "ACCOUNT_DISABLED",
                "This account has been deactivated. Contact an administrator to restore access.");
    }
}
