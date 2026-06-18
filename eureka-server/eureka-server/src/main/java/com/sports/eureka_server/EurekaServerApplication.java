package com.sports.eureka_server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Acts as the central directory where all microservices register themselves
 * on startup and renew their registration via periodic heartbeats (every 30s).
 *
 * How other services use it:
 *   - On startup: each service sends a POST to Eureka with its name, IP, and port
 *   - At runtime:  when Service A needs to call Service B, it asks Eureka
 *                  "Where is SERVICE-B?" and gets back the real host:port
 *   - This is called Service Discovery and enables lb://SERVICE-NAME URLs
 */
@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {

    private static final Logger log = LoggerFactory.getLogger(EurekaServerApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
        log.info("Eureka Server started. Dashboard: http://localhost:8761");
    }
}
