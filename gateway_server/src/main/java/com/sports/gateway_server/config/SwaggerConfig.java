package com.sports.gateway_server.config;

import org.springdoc.core.properties.AbstractSwaggerUiConfigProperties.SwaggerUrl;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

/**
 * Aggregates Swagger UI docs from all downstream microservices into one UI
 * accessible at: http://localhost:8080/swagger-ui.html
 *
 * Each service's OpenAPI JSON is fetched via the gateway routes defined in
 * application.properties under /swagger/{service-name}/v3/api-docs.
 *
 * Requirements:
 *  - Add springdoc-openapi-starter-webflux-ui to gateway's pom.xml
 *    (NOT webmvc-ui — gateway is reactive/WebFlux)
 *  - The /swagger/** routes in application.properties must be defined
 *    WITHOUT AuthFilter so the browser can load them unauthenticated.
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public SwaggerUiConfigProperties swaggerUiConfigProperties() {
        SwaggerUiConfigProperties config = new SwaggerUiConfigProperties();
        config.setUrls(Set.of(
            swaggerUrl("Auth Server",    "/swagger/auth-server/v3/api-docs"),
            swaggerUrl("Field Service",  "/swagger/field-service/v3/api-docs"),
            swaggerUrl("Booking Service","/swagger/booking-server/v3/api-docs"),
            swaggerUrl("Weather Service","/swagger/weather-server/v3/api-docs")
        ));
        return config;
    }

    private SwaggerUrl swaggerUrl(String name, String url) {
        SwaggerUrl s = new SwaggerUrl();
        s.setName(name);
        s.setUrl(url);
        return s;
    }
}
