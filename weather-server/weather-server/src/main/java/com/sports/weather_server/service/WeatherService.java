package com.sports.weather_server.service;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


@Service
public class WeatherService {

    private static final Logger log = LoggerFactory.getLogger(WeatherService.class);

    /** Maximum acceptable wind speed in km/h for outdoor sports */
    private static final double MAX_WIND_SPEED_KMH = 15.0;

    /** External weather API base URL */
    private static final String WEATHER_API_URL =
            "https://api.open-meteo.com/v1/forecast?latitude=%s&longitude=%s&current_weather=true";


    public boolean isWeatherSuitable(double lat, double lon) {
        log.info("Fetching weather data for lat={}, lon={}", lat, lon);

        try {
            String apiUrl = String.format(WEATHER_API_URL, lat, lon);
            log.debug("Calling external API: {}", apiUrl);

            // Open HTTP connection — using standard java.net for simplicity (no extra libs)
            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000); // 5s connection timeout
            conn.setReadTimeout(5000);    // 5s read timeout

            int responseCode = conn.getResponseCode();
            log.debug("External API response code: {}", responseCode);

            if (responseCode != 200) {
                log.warn("External weather API returned non-200 status: {}. Marking as BAD.", responseCode);
                return false;
            }

            // Read response body
            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }

            // Parse wind speed from JSON
            JSONObject json = new JSONObject(response.toString());
            JSONObject currentWeather = json.getJSONObject("current_weather");
            double windSpeed = currentWeather.getDouble("windspeed");

            log.info("Current wind speed at ({}, {}): {} km/h (threshold: {} km/h)",
                    lat, lon, windSpeed, MAX_WIND_SPEED_KMH);

            boolean suitable = windSpeed < MAX_WIND_SPEED_KMH;
            log.info("Weather suitability result: {}", suitable ? "GOOD" : "BAD");
            return suitable;

        } catch (Exception e) {
            // Network errors, parse failures — log and return false (deny booking for safety)
            log.error("Error calling external weather API: {}", e.getMessage(), e);
            return false;
        }
    }
}
