package com.sports.weather_server;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;


@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
@EnableDiscoveryClient
@OpenAPIDefinition(
    info = @Info(
        title       = "Weather Server API",
        version     = "1.0",
        description = "Checks real-time weather suitability for outdoor sports field bookings " +
                      "using the open-meteo.com API. Returns GOOD or BAD based on wind speed."
    )
)
public class WeatherServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(WeatherServerApplication.class, args);
    }
}
