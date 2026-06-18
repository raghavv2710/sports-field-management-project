package com.sports.field_server.controller;

import com.sports.field_server.entity.SportField;
import com.sports.field_server.service.FieldService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/fields")
@Tag(name = "Sports Fields", description = "Create, read, update, and delete sports fields")
public class FieldController {

    private static final Logger log = LoggerFactory.getLogger(FieldController.class);

    @Autowired
    private FieldService fieldService;
    
//    -----------------------------------------------

    @Operation(summary = "Create a new sports field",
               description = "Adds a new field to the catalog. All fields are mandatory.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Field created successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error — check request body")
    })
    @PostMapping
    public ResponseEntity<SportField> createField(@Valid @RequestBody SportField field) {
        log.info("Creating new field: name='{}', type='{}', indoor={}, pricePerHour = {}", field.getName(), field.getType(), field.isIndoor(), field.getPricePerHour());
        SportField saved = fieldService.addField(field);
        log.info("Field created with id={}", saved.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

//    ---------------------------------------------

    @Operation(summary = "Get all sports fields",
               description = "Returns the complete list of registered sports fields.")
    @ApiResponse(responseCode = "200", description = "List of fields returned")
    @GetMapping
    public List<SportField> getAllFields() {
        log.info("Fetching all sports fields");
        List<SportField> fields = fieldService.getAllFields();
        log.info("Returning {} field(s)", fields.size());
        return fields;
    }
    
//    -----------------------------------------

    @Operation(summary = "Get a field by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Field found"),
        @ApiResponse(responseCode = "404", description = "No field with that ID exists")
    })
    @GetMapping("/{id}")
    public ResponseEntity<SportField> getFieldById(
            @Parameter(description = "ID of the field to retrieve") @PathVariable Long id) {
        log.info("Fetching field with id={}", id);
        return ResponseEntity.ok(fieldService.getFieldById(id));
    }

//    ----------------------------------------

    @Operation(summary = "Update a field by ID",
               description = "Replaces all updatable fields (name, type, indoor, pricePerHour).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Field updated successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "404", description = "Field not found")
    })
    @PutMapping("/{id}")
    public ResponseEntity<SportField> updateField(
            @Parameter(description = "ID of the field to update") @PathVariable Long id,
            @Valid @RequestBody SportField fieldDetails) {
        log.info("Updating field id={}", id);
        SportField updated = fieldService.updateField(id, fieldDetails);
        log.info("Field id={} updated successfully", id);
        return ResponseEntity.ok(updated);
    }

//    -------------------------------------
    
    @Operation(summary = "Delete a field by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Field deleted"),
        @ApiResponse(responseCode = "404", description = "Field not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteField(
            @Parameter(description = "ID of the field to delete") @PathVariable Long id) {
        log.info("Deleting field id={}", id);
        fieldService.deleteField(id);
        log.info("Field id={} deleted successfully", id);
        return ResponseEntity.ok("Field deleted successfully.");
    }
}
