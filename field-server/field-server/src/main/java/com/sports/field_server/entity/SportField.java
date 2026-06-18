package com.sports.field_server.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;


@Entity
@Table(name = "sport_fields")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Sports field entity — court, ground, arena etc.")
public class SportField {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Auto-generated field ID", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotBlank(message = "Field name must not be blank")
    @Size(max = 100, message = "Field name must not exceed 100 characters")
    @Column(nullable = false)
    @Schema(description = "Display name of the field", example = "Football Ground A")
    private String name;

    @NotBlank(message = "Field type must not be blank")
    @Column(nullable = false)
    @Schema(description = "Type of sport played here", example = "Soccer")
    private String type;

  
    @Column(name = "is_indoor", nullable = false)
    @Schema(description = "True if indoors — skips weather check on booking", example = "false")
    private boolean indoor;

    @Positive(message = "Price per hour must be a positive number")
    @Column(nullable = false)
    @Schema(description = "Booking price per hour in INR", example = "800.0")
    private double pricePerHour;
}
