package com.sports.auth_server.controller;

import com.sports.auth_server.dto.LoginRequest;
import com.sports.auth_server.entity.UserCredential;
import com.sports.auth_server.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "User registration, login, and token validation endpoints")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthService service;

    @Autowired
    private AuthenticationManager authenticationManager;
    
//    ----------------------------------------

    @Operation(summary = "Register a new user (ROLE_USER)",
               description = "Creates a new account with ROLE_USER. Can view fields and make bookings.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User registered successfully"),
        @ApiResponse(responseCode = "400", description = "Validation failed"),
        @ApiResponse(responseCode = "500", description = "Username already exists")
    })
    @PostMapping("/register")
    public String addNewUser(@Valid @RequestBody UserCredential user) {
        log.info("Registration request for username='{}'", user.getName());
        String result = service.saveUser(user);
        log.info("User '{}' registered as ROLE_USER", user.getName());
        return result;
    }

//  ----------------------------------------
    
    @Operation(summary = "Register a new admin user",
               description = "Creates a ROLE_ADMIN account. Requires an existing admin JWT token.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Admin registered successfully"),
        @ApiResponse(responseCode = "400", description = "Validation failed"),
        @ApiResponse(responseCode = "403", description = "Access denied — only admins can register new admins"),
        @ApiResponse(responseCode = "500", description = "Username already exists")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/register/admin")
    public String addAdminUser(@Valid @RequestBody UserCredential user) {
        log.info("Admin registration request for username='{}'", user.getName());
        user.setRole("ROLE_ADMIN");
        String result = service.saveUser(user);
        log.info("Admin '{}' registered successfully", user.getName());
        return result;
    }

//  ----------------------------------------
    
    @Operation(summary = "Login and get JWT token",
               description = "Authenticates with name + password only (email not required). " +
                             "Returns a JWT token valid for 1 hour. Use as: Authorization: Bearer <token>")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login successful — JWT token returned"),
        @ApiResponse(responseCode = "400", description = "Blank name or password"),
        @ApiResponse(responseCode = "401", description = "Invalid username or password")
    })
    @PostMapping("/token")
    public String getToken(@Valid @RequestBody LoginRequest authRequest) {
        log.info("Token request for username='{}'", authRequest.getName());

        Authentication authenticate = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                authRequest.getName(),
                authRequest.getPassword()
            )
        );

        if (authenticate.isAuthenticated()) {
            String token = service.generateToken(authRequest.getName());
            log.info("JWT token generated for username='{}'", authRequest.getName());
            return token;
        } else {
            log.warn("Authentication failed for username='{}'", authRequest.getName());
            throw new RuntimeException("Invalid credentials");
        }
    }
    
//  ----------------------------------------

    @Operation(summary = "Validate a JWT token",
               description = "Called by the API Gateway AuthFilter. Returns 200 if valid, 401 if expired/invalid.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Token is valid"),
        @ApiResponse(responseCode = "401", description = "Token expired, tampered, or malformed")
    })
    @GetMapping("/validate")
    public String validateToken(@RequestParam("token") String token) {
        log.debug("Token validation request received");
        service.validateToken(token);
        log.debug("Token validated successfully");
        return "Token is valid";
    }
    
//  ----------------------------------------
    
    @Operation(
        summary = "Activate or deactivate a user account (Admin only)",
        description = "Set active=false to block a user from logging in. " +
                      "Set active=true to restore access. Admins cannot be deactivated."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Status updated successfully"),
        @ApiResponse(responseCode = "403", description = "Caller is not an admin"),
        @ApiResponse(responseCode = "500", description = "User not found or trying to deactivate an admin")
    })
    @PatchMapping("/users/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public String updateUserStatus(
            @PathVariable int id,
            @RequestParam boolean active) {

        log.info("Admin request to set user id={} active={}", id, active);

        return service.updateUserStatus(id, active);
    }
    
//    ----------------------------------------
    
    @GetMapping("/users")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public List<UserCredential> getUsers() {
        return service.getAllUsers();
    }
    
//    ----------------------------------------

    @PutMapping("/make-admin/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public UserCredential makeAdmin(@PathVariable Integer id) {
        return service.makeAdmin(id);
    }

}
