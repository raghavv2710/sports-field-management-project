package com.sports.auth_server.config;

import com.sports.auth_server.entity.UserCredential;
import com.sports.auth_server.repository.UserCredentialRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Custom implementation of Spring Security's UserDetailsService.
 *
 * Called by DaoAuthenticationProvider during the authentication flow
 * (i.e., when /auth/token is invoked). Loads a UserCredential from the
 * database by username and wraps it in a CustomUserDetails object.
 *
 * This is the bridge between the Spring Security authentication framework
 * and our own UserCredential JPA entity.
 */
@Component
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(CustomUserDetailsService.class);

    @Autowired
    private UserCredentialRepository repository;

    /**
     * Loads a user by their username for authentication.
     *
     * @param username the username from the login request
     * @return a UserDetails object wrapping the found UserCredential
     * @throws UsernameNotFoundException if no user with that name exists
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Loading user by username='{}'", username);

        Optional<UserCredential> credential = repository.findByName(username);

        return credential.map(c -> {
            log.debug("User '{}' found in database", username);
            return new CustomUserDetails(c);
        }).orElseThrow(() -> {
            log.warn("User not found: '{}'", username);
            return new UsernameNotFoundException("User not found: " + username);
        });
    }
}
