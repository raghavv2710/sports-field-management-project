package com.sports.booking_server.controller;

import com.sports.booking_server.entity.Booking;
import com.sports.booking_server.service.BookingService;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;


@RestController
@RequestMapping("/api/bookings")
@Tag(name = "Bookings", description = "Create bookings and view reservations for sports fields")
public class BookingController {

    private static final Logger log = LoggerFactory.getLogger(BookingController.class);

    @Autowired
    private BookingService bookingService;

//    -------------------------------------------
    
    @Operation(
        summary = "Book a sports field",
        description = """
            Validates and confirms a booking in 4 steps:
            1. Check the slot is not already taken (409 Conflict if taken)
            2. Fetch field details from FIELD-SERVICE
            3. If outdoor: check weather via WEATHER-SERVER (422 if BAD)
            4. Persist and return the confirmed booking
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Booking confirmed"),
        @ApiResponse(responseCode = "400", description = "Validation error — check request body fields"),
        @ApiResponse(responseCode = "409", description = "Slot already booked — choose a different time"),
        @ApiResponse(responseCode = "422", description = "Outdoor field booking rejected due to bad weather"),
        @ApiResponse(responseCode = "500", description = "FIELD-SERVICE unreachable or field ID does not exist")
    })
    @PostMapping
    public ResponseEntity<Booking> bookField(@Valid @RequestBody Booking booking) {
        log.info("Booking request: fieldId={}, date={}, hour={}, client='{}'",
                booking.getFieldId(), booking.getBookingDate(), booking.getStartHour(), booking.getClientName());
        Booking saved = bookingService.createBooking(booking);
        log.info("Booking created: id={}", saved.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

//    -------------------------------------------

    @Operation(
        summary = "View bookings for a field on a date",
        description = "Returns all confirmed bookings for the specified field and date. " +
                      "Use this to see which hours are already taken."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "List of bookings (may be empty)"),
        @ApiResponse(responseCode = "400", description = "Invalid date format — use YYYY-MM-DD")
    })
    @GetMapping("/view/{fieldId}")
    public List<Booking> viewReservations(
            @Parameter(description = "ID of the field to view bookings for") @PathVariable Long fieldId,
            @Parameter(description = "Date to query, format: YYYY-MM-DD", example = "2026-05-15")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        log.info("View reservations request: fieldId={}, date={}", fieldId, date);
        return bookingService.getReservationsByDate(fieldId, date);
    }
    
//    -------------------------------------------
    @Hidden   // exclude from Swagger UI — internal endpoint only
    @DeleteMapping("/field/{fieldId}")
    public ResponseEntity<String> deleteBookingsByFieldId(@PathVariable Long fieldId) {
        log.info("Internal request: delete all bookings for fieldId={}", fieldId);
        int deleted = bookingService.deleteBookingsByFieldId(fieldId);
        log.info("Deleted {} booking(s) for fieldId={}", deleted, fieldId);
        return ResponseEntity.ok(deleted + " booking(s) deleted for fieldId=" + fieldId);
    }
}
