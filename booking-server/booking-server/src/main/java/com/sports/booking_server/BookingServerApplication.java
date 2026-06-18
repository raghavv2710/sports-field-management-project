package com.sports.booking_server;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;
import java.util.TimeZone;

/**
 * Booking Server — Core business logic microservice.
 *
 * Orchestrates the 4-step booking pipeline:
 *   1. Availability check (local booking_db query)
 *   2. Field details via OpenFeign → FIELD-SERVICE
 *   3. Weather check via RestTemplate → WEATHER-SERVER (outdoor only)
 *   4. Persist confirmed booking
 *
 * FIX: Added TimeZone.setDefault(IST) so @FutureOrPresent validation
 *      on bookingDate works correctly in Asia/Kolkata timezone.
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@OpenAPIDefinition(
    info = @Info(
        title       = "Booking Server API",
        version     = "1.0",
        description = "Sports field booking with availability, field, and weather validation."
    )
)
public class BookingServerApplication {

    public static void main(String[] args) {
        // JVM default timezone to IST so @FutureOrPresent on bookingDate
        // evaluates correctly — without this, same-day bookings can fail if JVM is UTC.
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
        SpringApplication.run(BookingServerApplication.class, args);
    }

    /**
     * @LoadBalanced RestTemplate: resolves http://WEATHER-SERVER/... URLs
     * via Eureka. Without @LoadBalanced, WEATHER-SERVER would be treated
     * as a DNS hostname and cause UnknownHostException.
     */
    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
