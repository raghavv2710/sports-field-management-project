package com.sports.auth_server.service;

import com.sports.auth_server.entity.UserCredential;
import com.sports.auth_server.repository.UserCredentialRepository;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private UserCredentialRepository repository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;
    
    public List<UserCredential> getAllUsers() {
        return repository.findAll();
    }
    
    public UserCredential makeAdmin(Integer id) {
        UserCredential user = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setRole("ROLE_ADMIN");
        return repository.save(user);
    }

    public UserCredential updateUserStatus(Integer id, boolean active) {
        UserCredential user = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setActive(active);
        return repository.save(user);
    }


//    Encodes the plain-text password with BCrypt and persists the user.

    public String saveUser(UserCredential credential) {
        log.debug("Hashing password and saving user: {}", credential.getName());
        // BCrypt hashing: original plain-text password is replaced with hash
        credential.setPassword(passwordEncoder.encode(credential.getPassword()));
        repository.save(credential);
        log.info("User '{}' saved to database", credential.getName());
        return "User added to the system successfully";
    }


//    Generates a signed JWT token for the given username.

    public String generateToken(String username) {
        log.debug("Generating JWT token for user: {}", username);
        UserCredential user = repository.findByName(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return jwtService.generateToken(username, user.getRole());
    }

    public void validateToken(String token) {
        log.debug("Delegating token validation to JwtService");
        jwtService.validateToken(token);
    }
    
//    public String saveFirstAdmin(UserCredential credential) {
//        // Check if any admin already exists in the database
//        boolean adminAlreadyExists = repository.existsByRole("ROLE_ADMIN");
//
//        if (adminAlreadyExists) {
//            log.warn("Attempt to call /register/first-admin when admin already exists");
//            throw new IllegalStateException(
//                "An admin account already exists. " +
//                "To create additional admins, use POST /auth/register/admin " +
//                "with an Authorization: Bearer <admin-token> header."
//            );
//        }
//
//        // No admin exists yet — safe to create the first one
//        credential.setRole("ROLE_ADMIN");
//        credential.setPassword(passwordEncoder.encode(credential.getPassword()));
//        repository.save(credential);
//        log.info("First admin '{}' created successfully", credential.getName());
//        return "First admin created successfully. " +
//               "This endpoint is now permanently locked.";
//    }
    
    public String updateUserStatus(int userId, boolean active) {
        UserCredential user = repository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        // Prevent admins from being deactivated
        if ("ROLE_ADMIN".equals(user.getRole())) {
            throw new IllegalStateException(
                "Admin accounts cannot be deactivated. " +
                "Deactivate is only allowed for ROLE_USER accounts."
            );
        }

        user.setActive(active);
        repository.save(user);

        String status = active ? "activated" : "deactivated";
        log.info("User '{}' (id={}) has been {}", user.getName(), userId, status);
        return "User '" + user.getName() + "' has been " + status + " successfully.";
    }
    
}
