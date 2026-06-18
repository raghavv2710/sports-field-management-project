package com.sports.gateway_server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;


@SpringBootApplication
@EnableDiscoveryClient
public class GatewayServerApplication {

    private static final Logger log = LoggerFactory.getLogger(GatewayServerApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(GatewayServerApplication.class, args);
        log.info("API Gateway started on port 8080");
    }


    @Bean
    @LoadBalanced
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}
