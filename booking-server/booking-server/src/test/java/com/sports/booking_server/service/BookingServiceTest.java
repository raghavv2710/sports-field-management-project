package com.sports.booking_server.service;

import com.sports.booking_server.client.FieldClient;
import com.sports.booking_server.client.WeatherClient;
import com.sports.booking_server.dto.FieldDTO;
import com.sports.booking_server.entity.Booking;
import com.sports.booking_server.exception.FieldAlreadyBookedException;
import com.sports.booking_server.exception.WeatherConditionException;
import com.sports.booking_server.repository.BookingRepository;
import com.sports.booking_server.service.BookingService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * Unit tests for the BookingService 4-step pipeline.
 *
 * Key Mockito concepts used here:
 *   given(mock.method()).willReturn(value) — stub a return value
 *   then(mock).should(never()).method()    — assert a method was NEVER called
 *   then(mock).should(times(1)).method()  — assert a method was called once
 *
 * Each test focuses on ONE step failing to verify fail-fast behaviour.
 */
@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private FieldClient fieldClient;
    @Mock private WeatherClient weatherClient;

    @InjectMocks
    private BookingService bookingService;

    // ── Helper builders ───────────────────────────────────────────────────

    private Booking buildBooking(Long fieldId, LocalDate date, int hour) {
        return new Booking(null, fieldId, "John Doe", "9876543210",
                "john@test.com", date, hour);
    }

    private FieldDTO outdoorField() {
        return new FieldDTO(1L, "Football Ground A", "Soccer", false, 800.0);
    }

    private FieldDTO indoorField() {
        return new FieldDTO(2L, "Badminton Court", "Badminton", true, 400.0);
    }

    // ── STEP 1: Availability Check ────────────────────────────────────────

    @Test
    @DisplayName("Step 1: throws FieldAlreadyBookedException when slot is taken")
    void createBooking_slotTaken_throwsFieldAlreadyBookedException() {
        Booking booking = buildBooking(1L, LocalDate.of(2026, 12, 15), 14);
        given(bookingRepository.existsByFieldIdAndBookingDateAndStartHour(1L,
                LocalDate.of(2026, 12, 15), 14)).willReturn(true);

        assertThatThrownBy(() -> bookingService.createBooking(booking))
                .isInstanceOf(FieldAlreadyBookedException.class)
                .hasMessageContaining("Field 1")
                .hasMessageContaining("14:00");

        // If slot is taken, fieldClient and weatherClient must NOT be called (fail-fast)
        then(fieldClient).should(never()).getFieldById(anyLong());
        then(weatherClient).should(never()).isWeatherSuitable(anyDouble(), anyDouble());
    }

    // ── STEP 2: Field Lookup (Feign) ──────────────────────────────────────

    @Test
    @DisplayName("Step 2: fetches field details via FieldClient")
    void createBooking_slotAvailable_callsFieldClient() {
        Booking booking = buildBooking(1L, LocalDate.of(2026, 12, 15), 14);
        given(bookingRepository.existsByFieldIdAndBookingDateAndStartHour(any(), any(), anyInt()))
                .willReturn(false);
        given(fieldClient.getFieldById(1L)).willReturn(outdoorField());
        given(weatherClient.isWeatherSuitable(anyDouble(), anyDouble())).willReturn(true);
        given(bookingRepository.save(any())).willReturn(booking);

        bookingService.createBooking(booking);

        then(fieldClient).should(times(1)).getFieldById(1L);
    }

    @Test
    @DisplayName("Step 2: throws RuntimeException when field does not exist in field-server")
    void createBooking_fieldNotFound_throwsException() {
        Booking booking = buildBooking(999L, LocalDate.of(2026, 12, 15), 10);
        given(bookingRepository.existsByFieldIdAndBookingDateAndStartHour(any(), any(), anyInt()))
                .willReturn(false);
        given(fieldClient.getFieldById(999L))
                .willThrow(new RuntimeException("Field not found"));

        assertThatThrownBy(() -> bookingService.createBooking(booking))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Field not found");

        // Weather check must NOT be called if field lookup fails
        then(weatherClient).should(never()).isWeatherSuitable(anyDouble(), anyDouble());
    }

    // ── STEP 3: Weather Check (outdoor only) ─────────────────────────────

    @Test
    @DisplayName("Step 3: weather check called for outdoor field")
    void createBooking_outdoorField_callsWeatherClient() {
        Booking booking = buildBooking(1L, LocalDate.of(2026, 12, 15), 14);
        given(bookingRepository.existsByFieldIdAndBookingDateAndStartHour(any(), any(), anyInt()))
                .willReturn(false);
        given(fieldClient.getFieldById(1L)).willReturn(outdoorField());
        given(weatherClient.isWeatherSuitable(anyDouble(), anyDouble())).willReturn(true);
        given(bookingRepository.save(any())).willReturn(booking);

        bookingService.createBooking(booking);

        then(weatherClient).should(times(1)).isWeatherSuitable(anyDouble(), anyDouble());
    }

    @Test
    @DisplayName("Step 3: weather check SKIPPED for indoor field")
    void createBooking_indoorField_skipsWeatherClient() {
        Booking booking = buildBooking(2L, LocalDate.of(2026, 12, 15), 14);
        given(bookingRepository.existsByFieldIdAndBookingDateAndStartHour(any(), any(), anyInt()))
                .willReturn(false);
        given(fieldClient.getFieldById(2L)).willReturn(indoorField());
        given(bookingRepository.save(any())).willReturn(booking);

        bookingService.createBooking(booking);

        // indoor=true → WeatherClient must be completely skipped
        then(weatherClient).should(never()).isWeatherSuitable(anyDouble(), anyDouble());
    }

    @Test
    @DisplayName("Step 3: throws WeatherConditionException when weather is BAD for outdoor")
    void createBooking_outdoorBadWeather_throwsWeatherConditionException() {
        Booking booking = buildBooking(1L, LocalDate.of(2026, 12, 15), 14);
        given(bookingRepository.existsByFieldIdAndBookingDateAndStartHour(any(), any(), anyInt()))
                .willReturn(false);
        given(fieldClient.getFieldById(1L)).willReturn(outdoorField());
        given(weatherClient.isWeatherSuitable(anyDouble(), anyDouble())).willReturn(false);

        assertThatThrownBy(() -> bookingService.createBooking(booking))
                .isInstanceOf(WeatherConditionException.class)
                .hasMessageContaining("Outdoor booking rejected");

        // repository.save must NOT be called when weather is bad
        then(bookingRepository).should(never()).save(any());
    }

    // ── STEP 4: Persist ───────────────────────────────────────────────────

    @Test
    @DisplayName("Step 4: saves and returns the booking when all checks pass (outdoor)")
    void createBooking_allChecksPass_outdoor_savesAndReturns() {
        Booking booking = buildBooking(1L, LocalDate.of(2026, 12, 15), 14);
        Booking saved   = new Booking(42L, 1L, "John Doe", "9876543210",
                "john@test.com", LocalDate.of(2026, 12, 15), 14);

        given(bookingRepository.existsByFieldIdAndBookingDateAndStartHour(any(), any(), anyInt()))
                .willReturn(false);
        given(fieldClient.getFieldById(1L)).willReturn(outdoorField());
        given(weatherClient.isWeatherSuitable(anyDouble(), anyDouble())).willReturn(true);
        given(bookingRepository.save(booking)).willReturn(saved);

        Booking result = bookingService.createBooking(booking);

        assertThat(result.getId()).isEqualTo(42L);
        then(bookingRepository).should(times(1)).save(booking);
    }

    @Test
    @DisplayName("Step 4: saves and returns the booking when all checks pass (indoor)")
    void createBooking_allChecksPass_indoor_savesAndReturns() {
        Booking booking = buildBooking(2L, LocalDate.of(2026, 12, 15), 10);
        Booking saved   = new Booking(7L, 2L, "John Doe", "9876543210",
                "john@test.com", LocalDate.of(2026, 12, 15), 10);

        given(bookingRepository.existsByFieldIdAndBookingDateAndStartHour(any(), any(), anyInt()))
                .willReturn(false);
        given(fieldClient.getFieldById(2L)).willReturn(indoorField());
        given(bookingRepository.save(booking)).willReturn(saved);

        Booking result = bookingService.createBooking(booking);

        assertThat(result.getId()).isEqualTo(7L);
    }

    // ── getReservationsByDate ─────────────────────────────────────────────

    @Test
    @DisplayName("getReservationsByDate returns list from repository")
    void getReservationsByDate_returnsBookings() {
        LocalDate date = LocalDate.of(2026, 12, 15);
        List<Booking> bookings = List.of(
                buildBooking(1L, date, 10),
                buildBooking(1L, date, 14)
        );
        given(bookingRepository.findByFieldIdAndBookingDate(1L, date)).willReturn(bookings);

        List<Booking> result = bookingService.getReservationsByDate(1L, date);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Booking::getStartHour).containsExactly(10, 14);
    }

    @Test
    @DisplayName("getReservationsByDate returns empty list when no bookings exist")
    void getReservationsByDate_noBookings_returnsEmpty() {
        given(bookingRepository.findByFieldIdAndBookingDate(any(), any())).willReturn(List.of());

        assertThat(bookingService.getReservationsByDate(1L, LocalDate.now())).isEmpty();
    }
}
