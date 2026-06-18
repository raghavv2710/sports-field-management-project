package com.sports.booking_server.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Centralized exception handler for booking-server.
 *
 * HTTP status mapping:
 *   WeatherConditionException           → 422 Unprocessable Entity
 *   FieldAlreadyBookedException         → 409 Conflict
 *   MethodArgumentNotValidException     → 400 Bad Request  (bean validation)
 *   RuntimeException (Feign errors etc) → 500 Internal Server Error
 *
 * All responses follow the same JSON envelope:
 *   { timestamp, status, error, message }
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Weather conditions are unsuitable for outdoor booking. */
    @ExceptionHandler(WeatherConditionException.class)
    public ResponseEntity<Map<String, Object>> handleWeather(WeatherConditionException ex) {
        log.warn("WeatherConditionException: {}", ex.getMessage());
        return buildError(HttpStatus.UNPROCESSABLE_ENTITY, "WEATHER_CONDITION_ERROR", ex.getMessage());
    }

    /** The requested slot is already taken. */
    @ExceptionHandler(FieldAlreadyBookedException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(FieldAlreadyBookedException ex) {
        log.warn("FieldAlreadyBookedException: {}", ex.getMessage());
        return buildError(HttpStatus.CONFLICT, "BOOKING_CONFLICT", ex.getMessage());
    }

    /** Request body failed @Valid bean validation. Collects all field errors. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("Validation failed: {}", message);
        return buildError(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }

    /** Catch-all: Feign call failures, DB errors, null pointers, etc. */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntime(RuntimeException ex) {
        log.error("Unhandled exception in booking-server: {}", ex.getMessage(), ex);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
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
}
