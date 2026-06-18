package com.sports.auth_server.config;

import com.sports.auth_server.entity.UserCredential;
import com.sports.auth_server.repository.UserCredentialRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserCredentialRepository repository;

    @InjectMocks
    private CustomUserDetailsService service;

    @Test
    @DisplayName("loadUserByUsername returns UserDetails when user exists")
    void loadUserByUsername_found_returnsUserDetails() {
        UserCredential user = new UserCredential(1, "alice", "$2a$hash", "a@b.com", "ROLE_USER", false);
        given(repository.findByName("alice")).willReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("alice");

        assertThat(details.getUsername()).isEqualTo("alice");
        assertThat(details.getPassword()).isEqualTo("$2a$hash");
    }

    @Test
    @DisplayName("loadUserByUsername returns authority matching the user's role")
    void loadUserByUsername_found_authorityMatchesRole() {
        UserCredential admin = new UserCredential(2, "admin", "hash", "a@b.com", "ROLE_ADMIN", false);
        given(repository.findByName("admin")).willReturn(Optional.of(admin));

        UserDetails details = service.loadUserByUsername("admin");

        assertThat(details.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    @DisplayName("loadUserByUsername throws UsernameNotFoundException when user does not exist")
    void loadUserByUsername_notFound_throwsUsernameNotFoundException() {
        given(repository.findByName("ghost")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("ghost"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("ghost");
    }
}
