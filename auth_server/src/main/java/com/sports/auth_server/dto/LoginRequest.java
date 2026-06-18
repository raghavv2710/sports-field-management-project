package com.sports.auth_server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO used exclusively for the /auth/token (login) endpoint.
 *
 * Separates login from registration — login only needs name + password.
 * UserCredential also has email which is NOT required at login time.
 *
 * Using a dedicated DTO prevents @NotBlank @Email validation on email
 * from incorrectly rejecting login requests.
 */
@Schema(description = "Login credentials — name and password only")
public class LoginRequest {

    @NotBlank(message = "Username must not be blank")
    @Schema(description = "Registered username", example = "admin")
    private String name;

    @NotBlank(message = "Password must not be blank")
    @Schema(description = "Account password", example = "secret123")
    private String password;

    public LoginRequest() {}

    public LoginRequest(String name, String password) {
        this.name = name;
        this.password = password;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
