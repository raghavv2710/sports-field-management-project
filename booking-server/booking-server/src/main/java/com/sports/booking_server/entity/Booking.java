package com.sports.booking_server.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(
    name = "bookings",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_field_date_hour",
        columnNames = {"field_id", "booking_date", "start_hour"}
    )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "A confirmed booking for a sports field time slot")
public class Booking {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Auto-generated booking ID", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotNull(message = "Field ID must not be null")
    @Positive(message = "Field ID must be a positive number")
    @Column(name = "field_id", nullable = false)
    @Schema(description = "ID of the field to book (from field-server)", example = "1")
    private Long fieldId;

    @NotBlank(message = "Client name must not be blank")
    @Size(max = 100, message = "Client name must not exceed 100 characters")
    @Column(nullable = false)
    @Schema(description = "Full name of the person making the booking", example = "John Doe")
    private String clientName;

    @NotBlank(message = "Client phone must not be blank")
    @Pattern(regexp = "^\\+?[0-9]{7,15}$",
             message = "Phone number must be 7-15 digits, optionally starting with +")
    @Column(nullable = false)
    @Schema(description = "Contact phone number (7-15 digits)", example = "9876543210")
    private String clientPhone;

    @NotBlank(message = "Client email must not be blank")
    @Email(message = "Client email must be a valid email address")
    @Column(nullable = false)
    @Schema(description = "Contact email address", example = "john@example.com")
    private String clientEmail;


    @NotNull(message = "Booking date must not be null")
    @FutureOrPresent(message = "Booking date must be today or a future date")
    @Column(nullable = false)
    @Schema(description = "Booking date in ISO format YYYY-MM-DD", example = "2026-07-01")
    private LocalDate bookingDate;

  
    @Min(value = 0, message = "Start hour must be between 0 and 23")
    @Max(value = 23, message = "Start hour must be between 0 and 23")
    @Column(nullable = false)
    @Schema(description = "Hour of day for the booking (0-23). E.g. 14 = 2:00 PM to 3:00 PM.", example = "14")
    private int startHour;
}
