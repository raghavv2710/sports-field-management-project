package com.sports.auth_server.repository;

import com.sports.auth_server.entity.UserCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserCredentialRepository extends JpaRepository<UserCredential, Integer> {
    Optional<UserCredential> findByName(String username);

    boolean existsByRole(String role);
}