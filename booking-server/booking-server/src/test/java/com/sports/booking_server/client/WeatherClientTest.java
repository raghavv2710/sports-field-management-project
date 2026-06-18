package com.sports.booking_server.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import com.sports.booking_server.client.WeatherClient;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * Unit test for WeatherClient.
 *
 * We mock RestTemplate so no real HTTP call to WEATHER-SERVER is made.
 * This isolates WeatherClient's own logic: URL construction + response parsing.
 */
@ExtendWith(MockitoExtension.class)
class WeatherClientTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private WeatherClient weatherClient;

    @Test
    @DisplayName("isWeatherSuitable returns true when WEATHER-SERVER returns GOOD")
    void isWeatherSuitable_goodResponse_returnsTrue() {
        given(restTemplate.getForObject(anyString(), eq(String.class))).willReturn("GOOD");

        assertThat(weatherClient.isWeatherSuitable(12.97, 77.59)).isTrue();
    }

    @Test
    @DisplayName("isWeatherSuitable returns false when WEATHER-SERVER returns BAD")
    void isWeatherSuitable_badResponse_returnsFalse() {
        given(restTemplate.getForObject(anyString(), eq(String.class))).willReturn("BAD");

        assertThat(weatherClient.isWeatherSuitable(12.97, 77.59)).isFalse();
    }

    @Test
    @DisplayName("isWeatherSuitable returns false (safe default) when RestTemplate throws")
    void isWeatherSuitable_restTemplateThrows_returnsFalse() {
        given(restTemplate.getForObject(anyString(), eq(String.class)))
                .willThrow(new RuntimeException("Connection refused"));

        // Must not propagate exception — returns false (deny booking) when weather-server is down
        assertThat(weatherClient.isWeatherSuitable(12.97, 77.59)).isFalse();
    }

    @Test
    @DisplayName("isWeatherSuitable constructs URL with the provided lat/lon")
    void isWeatherSuitable_constructsCorrectUrl() {
        given(restTemplate.getForObject(anyString(), eq(String.class))).willReturn("GOOD");

        weatherClient.isWeatherSuitable(13.0, 80.0);

        // Cast the argument to String within the lambda to access .contains()
        then(restTemplate).should().getForObject(
                argThat((String url) -> url.contains("lat=13.0") && url.contains("lon=80.0")),
                eq(String.class)
        );
    }
}
