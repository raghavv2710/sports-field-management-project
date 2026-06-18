package com.sports.auth_server.service;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;

/**
 * Pure unit tests for JwtService.
 *
 * No Spring context loaded (@ExtendWith(MockitoExtension) would also work,
 * but since JwtService has no dependencies we just instantiate it directly).
 *
 * Why ReflectionTestUtils?
 *   JwtService reads its secret from @Value("${jwt.secret}").
 *   In a plain unit test there is no Spring context to inject @Value, so we
 *   inject the field value manually using ReflectionTestUtils.setField().
 */
class JwtServiceTest {

    // Same Base64-encoded key used in application.properties
    private static final String TEST_SECRET =
            "5367566B59703373367639792F423F4528482B4D6251655468576D5A71347437";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        // Inject the @Value field that Spring would normally set
        ReflectionTestUtils.setField(jwtService, "secret", TEST_SECRET);
    }

    // ── generateToken + validateToken ──────────────────────────────────────

    @Test
    @DisplayName("generateToken returns a non-blank JWT string")
    void generateToken_returnsNonBlankJwt() {
        String token = jwtService.generateToken("alice", "ROLE_USER");

        assertThat(token).isNotBlank();
        // JWT has exactly 3 dot-separated parts: header.payload.signature
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("validateToken does not throw for a freshly generated token")
    void validateToken_validToken_doesNotThrow() {
        String token = jwtService.generateToken("alice", "ROLE_USER");

        // assertThatCode verifies no exception is thrown
        assertThatCode(() -> jwtService.validateToken(token))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validateToken throws for a completely garbage string")
    void validateToken_malformedToken_throwsMalformedJwtException() {
        assertThatThrownBy(() -> jwtService.validateToken("not.a.jwt"))
                .isInstanceOf(MalformedJwtException.class);
    }

    @Test
    @DisplayName("validateToken throws for a token signed with a different secret")
    void validateToken_wrongSecret_throwsException() {
        // Create a second JwtService with a DIFFERENT key
        JwtService otherService = new JwtService();
        ReflectionTestUtils.setField(otherService, "secret",
                "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="); // different key

        // Token signed with TEST_SECRET cannot be verified with a different key
        String tokenFromOtherService = otherService.generateToken("bob", "ROLE_USER");

        assertThatThrownBy(() -> jwtService.validateToken(tokenFromOtherService))
                .isInstanceOf(RuntimeException.class); // SignatureException or JwtException
    }

    @Test
    @DisplayName("generateToken embeds the role in the payload")
    void generateToken_embedsRoleInPayload() {
        String adminToken = jwtService.generateToken("admin", "ROLE_ADMIN");
        String userToken  = jwtService.generateToken("alice", "ROLE_USER");

        // Decode payload (middle section, index 1) from Base64
        String adminPayload = new String(
                java.util.Base64.getDecoder().decode(adminToken.split("\\.")[1]));
        String userPayload  = new String(
                java.util.Base64.getDecoder().decode(userToken.split("\\.")[1]));

        assertThat(adminPayload).contains("ROLE_ADMIN");
        assertThat(userPayload).contains("ROLE_USER");
    }

    @Test
    @DisplayName("generateToken sets subject to the provided username")
    void generateToken_subjectIsUsername() {
        String token = jwtService.generateToken("charlie", "ROLE_USER");

        String payload = new String(
                java.util.Base64.getDecoder().decode(token.split("\\.")[1]));

        assertThat(payload).contains("charlie");
    }

    @Test
    @DisplayName("Two tokens for same user are different (different iat/exp)")
    void generateToken_twoCalls_produceDifferentTokens() throws InterruptedException {
        String t1 = jwtService.generateToken("alice", "ROLE_USER");
        Thread.sleep(1001); // ensure issuedAt milliseconds differ
        String t2 = jwtService.generateToken("alice", "ROLE_USER");

        assertThat(t1).isNotEqualTo(t2);
    }

    @Test
    @DisplayName("extractRole returns ROLE_ADMIN for admin token")
    void extractRole_adminToken_returnsRoleAdmin() {
        String token = jwtService.generateToken("admin", "ROLE_ADMIN");

        assertThat(jwtService.extractRole(token)).isEqualTo("ROLE_ADMIN");
    }

    @Test
    @DisplayName("extractRole returns ROLE_USER for user token")
    void extractRole_userToken_returnsRoleUser() {
        String token = jwtService.generateToken("alice", "ROLE_USER");

        assertThat(jwtService.extractRole(token)).isEqualTo("ROLE_USER");
    }
}
