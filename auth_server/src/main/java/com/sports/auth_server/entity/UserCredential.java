package com.sports.auth_server.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data               
@NoArgsConstructor  
@AllArgsConstructor 
@Schema(description = "User credential entity for registration and login")
public class UserCredential {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Auto-generated user ID", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private int id;

    @NotBlank(message = "Username must not be blank")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Column(unique = true, nullable = false)
    @Schema(description = "Unique username for login", example = "admin")
    private String name;

    @NotBlank(message = "Password must not be blank")
    @Size(min = 6, message = "Password must be at least 6 characters")
    @Column(nullable = false)
    @Schema(description = "Plain-text password (min 6 chars) — stored as BCrypt hash", example = "secret123")
    private String password;

    @NotBlank(message = "Email must not be blank")
    @Email(message = "Email must be a valid email address")
    @Column(nullable = false)
    @Schema(description = "Valid email address", example = "admin@sports.com")
    private String email;

    @Column(nullable = false)
    @Schema(description = "User role: ROLE_USER or ROLE_ADMIN", example = "ROLE_USER",
            accessMode = Schema.AccessMode.READ_ONLY)
    private String role = "ROLE_USER";
    //api/register/
    
    //Admins cannot be deactivated — enforced in AuthService.updateUserStatus()
    @Column(nullable = false)
    @Schema(description = "Account active status. false = login blocked.",
           example = "true", accessMode = Schema.AccessMode.READ_ONLY)
    private boolean active = true;
}
