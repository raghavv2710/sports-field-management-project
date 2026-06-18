package com.sports.booking_server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Field details received from field-server via Feign")
public class FieldDTO {


	@Schema(description = "Field ID", example = "1")
    private Long id;

    @Schema(description = "Field display name", example = "Football Ground A")
    private String name;

    @Schema(description = "Sport type", example = "Soccer")
    private String type;


    @Schema(description = "True if indoor — weather check skipped for indoor fields")
    private boolean indoor;

    @Schema(description = "Price per hour in INR", example = "800.0")
    private double pricePerHour;
}
