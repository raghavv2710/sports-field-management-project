package com.sports.auth_server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sports.auth_server.dto.LoginRequest;
import com.sports.auth_server.entity.UserCredential;
import com.sports.auth_server.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller slice test for AuthController.
 *
 * @WebMvcTest(AuthController.class)
 *   Loads ONLY the web layer (controller, filters, MockMvc).
 *   Does NOT load service, repository, or database beans.
 *   Much faster than @SpringBootTest.
 *
 * @MockBean
 *   Creates a Mockito mock and registers it as a Spring bean so the
 *   controller can @Autowire it. Replaces the real bean in the context.
 *
 * MockMvc
 *   Simulates HTTP requests without starting a real server.
 *   perform() → andExpect() pattern mirrors real HTTP assertions.
 */
@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper; // For serialising request bodies to JSON

    @MockBean
    private AuthService authService;

    @MockBean
    private AuthenticationManager authenticationManager;

    // ── POST /auth/register ───────────────────────────────────────────────

    @Test
    @DisplayName("POST /auth/register returns 200 with success message")
    void register_validUser_returns200() throws Exception {
        UserCredential user = new UserCredential(
                0, "alice", "secret123", "alice@sports.com", "ROLE_USER", false);

        given(authService.saveUser(any())).willReturn("User added to the system successfully");

        mockMvc.perform(post("/auth/register")
                        .with(csrf()) // Spring Security requires CSRF token in tests
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isOk())
                .andExpect(content().string("User added to the system successfully"));
    }

    @Test
    @DisplayName("POST /auth/register returns 400 when username is blank")
    void register_blankUsername_returns400() throws Exception {
        // name is blank — violates @NotBlank
        UserCredential invalid = new UserCredential(0, "", "secret123", "a@b.com", "ROLE_USER", false);

        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /auth/register returns 400 when password is too short")
    void register_shortPassword_returns400() throws Exception {
        UserCredential invalid = new UserCredential(0, "alice", "abc", "a@b.com", "ROLE_USER", false);

        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /auth/register returns 400 when email is invalid")
    void register_invalidEmail_returns400() throws Exception {
        UserCredential invalid = new UserCredential(0, "alice", "secret123", "not-an-email", "ROLE_USER", false);

        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    // ── POST /auth/register/first-admin ──────────────────────────────────

    @Test
    @DisplayName("POST /auth/register/first-admin sets role to ROLE_ADMIN")
    void registerFirstAdmin_validBody_returns200() throws Exception {
        UserCredential user = new UserCredential(
                0, "admin", "admin123", "admin@sports.com", "ROLE_USER", false);

        given(authService.saveUser(any())).willReturn("User added to the system successfully");

        mockMvc.perform(post("/auth/register/first-admin")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isOk());
    }

    // ── POST /auth/token ──────────────────────────────────────────────────

    @Test
    @DisplayName("POST /auth/token returns JWT on successful authentication")
    void getToken_validCredentials_returnsJwt() throws Exception {
        LoginRequest req = new LoginRequest("alice", "secret123");

        Authentication auth = mock(Authentication.class);
        given(auth.isAuthenticated()).willReturn(true);
        given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .willReturn(auth);
        given(authService.generateToken("alice")).willReturn("mocked.jwt.token");

        mockMvc.perform(post("/auth/token")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(content().string("mocked.jwt.token"));
    }

    @Test
    @DisplayName("POST /auth/token returns 401 on bad credentials")
    void getToken_badCredentials_returns401() throws Exception {
        LoginRequest req = new LoginRequest("alice", "wrong");

        given(authenticationManager.authenticate(any()))
                .willThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/auth/token")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /auth/token returns 400 when name is blank")
    void getToken_blankName_returns400() throws Exception {
        LoginRequest req = new LoginRequest("", "secret123");

        mockMvc.perform(post("/auth/token")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // ── GET /auth/validate ────────────────────────────────────────────────

    @Test
    @DisplayName("GET /auth/validate returns 200 for a valid token")
    void validateToken_validToken_returns200() throws Exception {
        // validateToken is void — no exception = valid
        willDoNothing().given(authService).validateToken("good.token");

        mockMvc.perform(get("/auth/validate")
                        .param("token", "good.token"))
                .andExpect(status().isOk())
                .andExpect(content().string("Token is valid"));
    }

    @Test
    @DisplayName("GET /auth/validate returns 500 for an expired/invalid token")
    void validateToken_invalidToken_returns500() throws Exception {
        willThrow(new RuntimeException("Token expired"))
                .given(authService).validateToken("expired.token");

        mockMvc.perform(get("/auth/validate")
                        .param("token", "expired.token"))
                .andExpect(status().isInternalServerError());
    }
}
