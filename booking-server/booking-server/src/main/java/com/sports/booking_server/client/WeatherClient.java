package com.sports.booking_server.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class WeatherClient {

    private static final Logger log = LoggerFactory.getLogger(WeatherClient.class);

    @Autowired
    private RestTemplate restTemplate; // @LoadBalanced bean from BookingServerApplication


    public boolean isWeatherSuitable(double lat, double lon) {
        // lb://WEATHER-SERVER is resolved to the real IP:port via Eureka
        String url = "http://WEATHER-SERVER/weather/check?lat=" + lat + "&lon=" + lon;
        log.info("Calling WEATHER-SERVER: lat={}, lon={}", lat, lon);

        try {
            String response = restTemplate.getForObject(url, String.class);
            log.info("WEATHER-SERVER response: '{}'", response);
            return "GOOD".equalsIgnoreCase(response);
        } catch (Exception e) {
            // If weather-server is unavailable, log and deny the booking for safety
            log.error("WEATHER-SERVER call failed: {}. Rejecting outdoor booking.", e.getMessage());
            return false;
        }
    }
}
