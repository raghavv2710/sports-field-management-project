package com.sports.weather_server.service;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.sports.weather_server.service.WeatherService;

import java.io.IOException;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for WeatherService.
 *
 * WeatherService uses java.net.HttpURLConnection to call the external
 * open-meteo.com API. To test it without hitting the real internet we use
 * OkHttp's MockWebServer, which starts a real local HTTP server on a random
 * port and lets us enqueue fake responses.
 *
 * Strategy:
 *   1. Start MockWebServer before each test
 *   2. Inject its URL into WeatherService via ReflectionTestUtils
 *      (replaces the private WEATHER_API_URL constant at runtime)
 *   3. Enqueue the response we want the "external API" to return
 *   4. Call weatherService.isWeatherSuitable() and assert the result
 *   5. Optionally verify the request that was made to MockWebServer
 */
class WeatherServiceTest {

    private MockWebServer mockWebServer;
    private WeatherService weatherService;

    // The real API URL format in WeatherService
    private static final String GOOD_RESPONSE = """
            {
              "current_weather": {
                "temperature": 28.3,
                "windspeed": 8.5,
                "winddirection": 180,
                "weathercode": 1,
                "is_day": 1
              }
            }
            """;

    private static final String BAD_RESPONSE = """
            {
              "current_weather": {
                "temperature": 22.0,
                "windspeed": 20.0,
                "winddirection": 90,
                "weathercode": 3,
                "is_day": 1
              }
            }
            """;

    private static final String EXACTLY_AT_THRESHOLD = """
            {
              "current_weather": {
                "temperature": 25.0,
                "windspeed": 15.0,
                "winddirection": 45,
                "weathercode": 0,
                "is_day": 1
              }
            }
            """;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start(); // Starts on a random available port

        weatherService = new WeatherService();

        // Replace the private static WEATHER_API_URL with one pointing to MockWebServer.
        // The format string must match what WeatherService.isWeatherSuitable uses:
        //   String.format(WEATHER_API_URL, lat, lon)
        String mockUrl = "http://localhost:" + mockWebServer.getPort()
                + "/v1/forecast?latitude=%s&longitude=%s&current_weather=true";

        ReflectionTestUtils.setField(
                WeatherService.class, "WEATHER_API_URL", mockUrl);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    // ── GOOD weather ──────────────────────────────────────────────────────

    @Test
    @DisplayName("Returns true (GOOD) when windspeed is below 15 km/h")
    void isWeatherSuitable_lowWind_returnsTrue() throws InterruptedException {
        // Enqueue the fake API response (windspeed: 8.5 < 15 threshold)
        mockWebServer.enqueue(new MockResponse()
                .setBody(GOOD_RESPONSE)
                .addHeader("Content-Type", "application/json")
                .setResponseCode(200));

        boolean result = weatherService.isWeatherSuitable(12.97, 77.59);

        assertThat(result).isTrue();
    }

    // ── BAD weather ───────────────────────────────────────────────────────

    @Test
    @DisplayName("Returns false (BAD) when windspeed is at or above 15 km/h")
    void isWeatherSuitable_highWind_returnsFalse() {
        // windspeed: 20.0 >= 15 threshold → BAD
        mockWebServer.enqueue(new MockResponse()
                .setBody(BAD_RESPONSE)
                .addHeader("Content-Type", "application/json")
                .setResponseCode(200));

        boolean result = weatherService.isWeatherSuitable(12.97, 77.59);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Returns false when windspeed is exactly 15 km/h (boundary value)")
    void isWeatherSuitable_exactlyAtThreshold_returnsFalse() {
        // 15.0 is NOT < 15.0, so result should be false
        mockWebServer.enqueue(new MockResponse()
                .setBody(EXACTLY_AT_THRESHOLD)
                .addHeader("Content-Type", "application/json")
                .setResponseCode(200));

        boolean result = weatherService.isWeatherSuitable(12.97, 77.59);

        assertThat(result).isFalse(); // boundary: must be strictly LESS than 15
    }

    // ── Error cases ───────────────────────────────────────────────────────

    @Test
    @DisplayName("Returns false (safe default) when external API returns non-200 status")
    void isWeatherSuitable_apiReturns500_returnsFalse() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        boolean result = weatherService.isWeatherSuitable(12.97, 77.59);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Returns false (safe default) when external API returns malformed JSON")
    void isWeatherSuitable_malformedJson_returnsFalse() {
        mockWebServer.enqueue(new MockResponse()
                .setBody("NOT_JSON_AT_ALL")
                .setResponseCode(200));

        // Must not propagate JSONException — returns false safely
        assertThatCode(() -> {
            boolean result = weatherService.isWeatherSuitable(12.97, 77.59);
            assertThat(result).isFalse();
        }).doesNotThrowAnyException();
    }

    // ── Request verification ──────────────────────────────────────────────

    @Test
    @DisplayName("Sends request with correct lat/lon query parameters")
    void isWeatherSuitable_sendsCorrectQueryParams() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
                .setBody(GOOD_RESPONSE)
                .setResponseCode(200));

        weatherService.isWeatherSuitable(13.0, 80.0);

        RecordedRequest request = mockWebServer.takeRequest();
        String path = request.getPath(); // e.g. /v1/forecast?latitude=13.0&longitude=80.0&...

        assertThat(path).contains("latitude=13.0");
        assertThat(path).contains("longitude=80.0");
        assertThat(path).contains("current_weather=true");
        assertThat(request.getMethod()).isEqualTo("GET");
    }
}
