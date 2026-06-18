package com.sports.field_server.repository;

import com.sports.field_server.entity.SportField;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FieldRepository extends JpaRepository<SportField, Long> {
}