package com.sports.weather_server.controller;

import com.sports.weather_server.service.WeatherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/weather")
@Tag(name = "Weather Check", description = "Check real-time weather suitability for outdoor sports")
public class WeatherController {

    private static final Logger log = LoggerFactory.getLogger(WeatherController.class);

    @Autowired
    private WeatherService weatherService;

    @Operation(
        summary = "Check weather suitability",
        description = "Returns GOOD if wind speed < 15 km/h at the given coordinates, BAD otherwise. " +
                      "Uses the free open-meteo.com API (no API key needed)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "GOOD or BAD (plain text)"),
        @ApiResponse(responseCode = "500", description = "External weather API call failed")
    })
    @GetMapping("/check")
    public String checkWeather(
            @Parameter(description = "Latitude of the field location", example = "12.97") @RequestParam double lat,
            @Parameter(description = "Longitude of the field location", example = "77.59") @RequestParam double lon) {
        log.info("Weather check request: lat={}, lon={}", lat, lon);
        boolean suitable = weatherService.isWeatherSuitable(lat, lon);
        String result = suitable ? "GOOD" : "BAD";
        log.info("Weather check result: {}", result);
        return result;
    }
}
