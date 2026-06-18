package com.sports.field_server.exception;


public class FieldNotFoundException extends RuntimeException {
    public FieldNotFoundException(Long id) {
        super("Field not found with id: " + id);
    }
}