package com.sports.field_server;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;


@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@OpenAPIDefinition(
    info = @Info(
        title       = "Field Server API",
        version     = "1.0",	
        description = "CRUD operations for managing sports fields (courts, grounds, etc.)"
    )
)
public class FieldServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(FieldServerApplication.class, args);
    }
}
