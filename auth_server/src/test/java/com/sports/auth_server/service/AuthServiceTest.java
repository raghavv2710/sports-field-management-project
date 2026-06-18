package com.sports.auth_server.service;

import com.sports.auth_server.entity.UserCredential;
import com.sports.auth_server.repository.UserCredentialRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * Unit tests for AuthService using Mockito.
 *
 * @ExtendWith(MockitoExtension.class)
 *   Activates Mockito annotations (@Mock, @InjectMocks) without Spring.
 *   Faster than @SpringBootTest because no application context is created.
 *
 * @Mock  — creates a fake (stub) of the annotated type
 * @InjectMocks — creates a real AuthService and injects all @Mock fields into it
 *
 * BDD (given/when/then) style:
 *   given(...).willReturn(...) → stub: "when this is called, return that"
 *   then(mock).should().method(...) → verify: "this method was called"
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserCredentialRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    // ── saveUser ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("saveUser encodes the password before saving")
    void saveUser_encodesPassword() {
        // GIVEN
        UserCredential user = new UserCredential(0, "alice", "plaintext", "a@b.com", "ROLE_USER", false);
        given(passwordEncoder.encode("plaintext")).willReturn("$2a$10$hashedvalue");
        given(repository.save(any())).willReturn(user);

        // WHEN
        String result = authService.saveUser(user);

        // THEN
        assertThat(result).isEqualTo("User added to the system successfully");
        // Verify the password on the entity was replaced with the hash
        assertThat(user.getPassword()).isEqualTo("$2a$10$hashedvalue");
        then(repository).should().save(user); // verify repository.save() was called once
    }

    @Test
    @DisplayName("saveUser calls passwordEncoder.encode with the original plain password")
    void saveUser_callsEncoderWithOriginalPassword() {
        UserCredential user = new UserCredential(0, "bob", "mypassword", "b@c.com", "ROLE_USER", false);
        given(passwordEncoder.encode(anyString())).willReturn("encoded");
        given(repository.save(any())).willReturn(user);

        authService.saveUser(user);

        // Verify encoder was called with the ORIGINAL plain-text password
        then(passwordEncoder).should().encode("mypassword");
    }

    // ── generateToken ─────────────────────────────────────────────────────

    @Test
    @DisplayName("generateToken returns the JWT string from JwtService")
    void generateToken_delegatesToJwtService() {
        // GIVEN — a user exists in the database
        UserCredential user = new UserCredential(1, "alice", "hash", "a@b.com", "ROLE_USER", false);
        given(repository.findByName("alice")).willReturn(Optional.of(user));
        given(jwtService.generateToken("alice", "ROLE_USER")).willReturn("mocked.jwt.token");

        // WHEN
        String token = authService.generateToken("alice");

        // THEN
        assertThat(token).isEqualTo("mocked.jwt.token");
        then(jwtService).should().generateToken("alice", "ROLE_USER");
    }

    @Test
    @DisplayName("generateToken throws RuntimeException when user not found")
    void generateToken_userNotFound_throwsException() {
        given(repository.findByName("ghost")).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.generateToken("ghost"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("generateToken passes the user's role to JwtService")
    void generateToken_passesRoleToJwtService() {
        UserCredential admin = new UserCredential(2, "admin", "hash", "a@b.com", "ROLE_ADMIN", false);
        given(repository.findByName("admin")).willReturn(Optional.of(admin));
        given(jwtService.generateToken("admin", "ROLE_ADMIN")).willReturn("admin.token");

        authService.generateToken("admin");

        // Verify role ROLE_ADMIN was passed, not ROLE_USER
        then(jwtService).should().generateToken("admin", "ROLE_ADMIN");
    }

    // ── validateToken ────────────────────────────────────────────────────

    @Test
    @DisplayName("validateToken delegates to JwtService without modification")
    void validateToken_delegatesToJwtService() {
        // JwtService.validateToken is void — no stubbing needed
        // We just verify it was called
        authService.validateToken("some.jwt.token");

        then(jwtService).should().validateToken("some.jwt.token");
    }

    @Test
    @DisplayName("validateToken propagates exception from JwtService")
    void validateToken_propagatesException() {
        // Stub jwtService to throw when called with an invalid token
        willThrow(new RuntimeException("Token expired"))
                .given(jwtService).validateToken("bad.token");

        assertThatThrownBy(() -> authService.validateToken("bad.token"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Token expired");
    }
}
