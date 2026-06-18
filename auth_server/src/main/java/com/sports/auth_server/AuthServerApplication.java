package com.sports.auth_server;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
Auth Server — Entry point for the authentication microservice.

Responsibilities:
 - User registration with BCrypt password hashing
 - JWT token generation on successful login
 - JWT token validation for the API Gateway's AuthFilter
 
Swagger UI: http://localhost:8084/swagger-ui/index.html

@OpenAPIDefinition: it is used to define global metadata for the entire API
When you build APIs, other developers need to know:

What is this API?
What does it do?
Who created it?
Which version is running?

Instead of writing documentation manually, OpenAPI lets you embed documentation directly in code.
 */

@SpringBootApplication
@EnableDiscoveryClient
@OpenAPIDefinition( // container annotation. It can hold the below information
    info = @Info( // holds General API details, defines how your API appears in Swagger UI.
        title       = "Auth Server API",
        version     = "1.0",
        description = "Handles user registration, JWT token generation, and token validation.",
        contact     = @Contact(name = "Sports Booking System")
    )
)

//Flow - springdoc scans Annotations → builds OpenAPI JSON → Swagger UI reads JSON → Displays Visual Documentation

public class AuthServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServerApplication.class, args);
    }
}
