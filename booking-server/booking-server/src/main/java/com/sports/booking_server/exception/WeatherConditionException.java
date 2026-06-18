package com.sports.booking_server.exception;

/**
 * Thrown when weather-server reports unsuitable conditions for an outdoor booking.
 *
 * HTTP mapping: 422 Unprocessable Entity
 *   The request is syntactically correct, the field exists, but business
 *   rules (wind speed threshold) prevent the booking from being confirmed.
 *
 * FIX: When wind speed is unknown (-1.0, because WeatherClient returns only
 *      a boolean), the message no longer shows the nonsensical "-1.0 km/h".
 *      Instead it shows a user-friendly message without a specific wind value.
 */
public class WeatherConditionException extends RuntimeException {

    /**
     * @param windSpeed the actual wind speed in km/h, or -1.0 if unknown
     */
    public WeatherConditionException(double windSpeed) {
        super(buildMessage(windSpeed));
    }

    private static String buildMessage(double windSpeed) {
        if (windSpeed < 0) {
            // Wind speed unknown — WeatherClient returns boolean, not the raw value
            return "Outdoor booking rejected: current weather conditions are unsuitable for outdoor play. " +
                   "Please try an indoor field or choose a different time.";
        }
        return String.format(
            "Outdoor booking rejected: wind speed is %.1f km/h which exceeds the safe limit of 15 km/h. " +
            "Please try an indoor field or choose a different time.",
            windSpeed
        );
    }
}
