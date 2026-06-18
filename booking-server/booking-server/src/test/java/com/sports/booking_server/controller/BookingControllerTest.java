package com.sports.booking_server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sports.booking_server.controller.BookingController;
import com.sports.booking_server.entity.Booking;
import com.sports.booking_server.exception.FieldAlreadyBookedException;
import com.sports.booking_server.exception.WeatherConditionException;
import com.sports.booking_server.service.BookingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookingController.class)
class BookingControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean  private BookingService bookingService;

    private Booking buildBooking(Long fieldId, LocalDate date, int hour) {
        return new Booking(null, fieldId, "John Doe", "9876543210",
                "john@test.com", date, hour);
    }

    // ── POST /api/bookings ────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/bookings returns 201 with saved booking")
    void bookField_valid_returns201() throws Exception {
        Booking input = buildBooking(1L, LocalDate.of(2026, 12, 15), 14);
        Booking saved = new Booking(1L, 1L, "John Doe", "9876543210",
                "john@test.com", LocalDate.of(2026, 12, 15), 14);

        given(bookingService.createBooking(any())).willReturn(saved);

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.clientName").value("John Doe"))
                .andExpect(jsonPath("$.startHour").value(14));
    }

    @Test
    @DisplayName("POST /api/bookings returns 409 when slot already booked")
    void bookField_slotTaken_returns409() throws Exception {
        Booking input = buildBooking(1L, LocalDate.of(2026, 12, 15), 14);

        given(bookingService.createBooking(any()))
                .willThrow(new FieldAlreadyBookedException(1L, "2026-12-15", 14));

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("BOOKING_CONFLICT"));
    }

    @Test
    @DisplayName("POST /api/bookings returns 422 when weather is bad for outdoor field")
    void bookField_badWeather_returns422() throws Exception {
        Booking input = buildBooking(1L, LocalDate.of(2026, 12, 15), 14);

        given(bookingService.createBooking(any()))
                .willThrow(new WeatherConditionException(-1.0));

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("WEATHER_CONDITION_ERROR"));
    }

    @Test
    @DisplayName("POST /api/bookings returns 400 when clientName is blank")
    void bookField_blankClientName_returns400() throws Exception {
        // Blank clientName violates @NotBlank
        Booking invalid = new Booking(null, 1L, "", "9876543210",
                "j@t.com", LocalDate.of(2026, 12, 15), 14);

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /api/bookings returns 400 when phone number is invalid format")
    void bookField_invalidPhone_returns400() throws Exception {
        // Phone "abc" does not match ^\\+?[0-9]{7,15}$
        Booking invalid = new Booking(null, 1L, "John", "abc",
                "j@t.com", LocalDate.of(2026, 12, 15), 14);

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/bookings returns 400 when startHour is out of range")
    void bookField_startHourOutOfRange_returns400() throws Exception {
        // startHour=25 violates @Max(23)
        Booking invalid = new Booking(null, 1L, "John", "9876543210",
                "j@t.com", LocalDate.of(2026, 12, 15), 25);

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    // ── GET /api/bookings/view/{fieldId} ──────────────────────────────────

    @Test
    @DisplayName("GET /api/bookings/view/{fieldId} returns list of bookings")
    void viewReservations_returns200WithList() throws Exception {
        LocalDate date = LocalDate.of(2026, 12, 15);
        List<Booking> bookings = List.of(
                new Booking(1L, 1L, "Alice", "1111111111", "a@b.com", date, 10),
                new Booking(2L, 1L, "Bob",   "2222222222", "b@c.com", date, 14)
        );
        given(bookingService.getReservationsByDate(1L, date)).willReturn(bookings);

        mockMvc.perform(get("/api/bookings/view/1")
                        .param("date", "2026-12-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].clientName").value("Alice"))
                .andExpect(jsonPath("$[1].startHour").value(14));
    }

    @Test
    @DisplayName("GET /api/bookings/view/{fieldId} returns empty array when no bookings")
    void viewReservations_noBookings_returnsEmptyArray() throws Exception {
        given(bookingService.getReservationsByDate(any(), any())).willReturn(List.of());

        mockMvc.perform(get("/api/bookings/view/1")
                        .param("date", "2026-12-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
