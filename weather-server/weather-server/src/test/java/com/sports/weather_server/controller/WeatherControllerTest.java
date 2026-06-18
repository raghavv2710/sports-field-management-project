package com.sports.weather_server.controller;

import com.sports.weather_server.controller.WeatherController;
import com.sports.weather_server.service.WeatherService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WeatherController.class)
class WeatherControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean  private WeatherService weatherService;

    @Test
    @DisplayName("GET /weather/check returns GOOD when weather is suitable")
    void checkWeather_suitable_returnsGOOD() throws Exception {
        given(weatherService.isWeatherSuitable(12.97, 77.59)).willReturn(true);

        mockMvc.perform(get("/weather/check")
                        .param("lat", "12.97")
                        .param("lon", "77.59"))
                .andExpect(status().isOk())
                .andExpect(content().string("GOOD"));
    }

    @Test
    @DisplayName("GET /weather/check returns BAD when weather is not suitable")
    void checkWeather_notSuitable_returnsBAD() throws Exception {
        given(weatherService.isWeatherSuitable(12.97, 77.59)).willReturn(false);

        mockMvc.perform(get("/weather/check")
                        .param("lat", "12.97")
                        .param("lon", "77.59"))
                .andExpect(status().isOk())
                .andExpect(content().string("BAD"));
    }

    @Test
    @DisplayName("GET /weather/check returns 400 when lat is not a number")
    void checkWeather_invalidLat_returns400() throws Exception {
        mockMvc.perform(get("/weather/check")
                        .param("lat", "NOT_A_NUMBER")
                        .param("lon", "77.59"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /weather/check returns 500 when WeatherService throws")
    void checkWeather_serviceThrows_returns500() throws Exception {
        given(weatherService.isWeatherSuitable(anyDouble(), anyDouble()))
                .willThrow(new RuntimeException("External API timeout"));

        mockMvc.perform(get("/weather/check")
                        .param("lat", "12.97")
                        .param("lon", "77.59"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("WEATHER_SERVICE_ERROR"));
    }

    @Test
    @DisplayName("GET /weather/check delegates correct lat/lon to WeatherService")
    void checkWeather_passesCoordinatesToService() throws Exception {
        given(weatherService.isWeatherSuitable(13.5, 80.1)).willReturn(true);

        mockMvc.perform(get("/weather/check")
                        .param("lat", "13.5")
                        .param("lon", "80.1"))
                .andExpect(status().isOk());

        then(weatherService).should().isWeatherSuitable(13.5, 80.1);
    }
}
