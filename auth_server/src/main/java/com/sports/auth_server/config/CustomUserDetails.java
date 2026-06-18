package com.sports.auth_server.config;

import com.sports.auth_server.entity.UserCredential;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * Custom implementation of UserDetails that pulls user data from DB.
 *
 * FIX:
 * - Uses role from UserCredential instead of hardcoding ROLE_ADMIN
 * - Enables proper Role-Based Access Control (RBAC)
 */
public class CustomUserDetails implements UserDetails {

    private final UserCredential credential;

    public CustomUserDetails(UserCredential credential) {
        this.credential = credential;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(
            new SimpleGrantedAuthority(credential.getRole())
        );
    }

    @Override
    public String getPassword() {
        return credential.getPassword();
    }

    @Override
    public String getUsername() {
        return credential.getName();
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() {
        return credential.isActive();  
    }
}