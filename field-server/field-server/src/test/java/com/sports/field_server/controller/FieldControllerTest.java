package com.sports.field_server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sports.field_server.controller.FieldController;
import com.sports.field_server.entity.SportField;
import com.sports.field_server.exception.FieldNotFoundException;
import com.sports.field_server.service.FieldService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Field-server has no Spring Security, so no csrf() or auth headers needed.
 * @WebMvcTest loads only the controller layer + MockMvc.
 */
@WebMvcTest(FieldController.class)
class FieldControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FieldService fieldService;

    private SportField buildField(Long id, String name, String type, boolean indoor, double price) {
        return new SportField(id, name, type, indoor, price);
    }

    // ── POST /api/fields ──────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/fields returns 201 with saved field")
    void createField_valid_returns201() throws Exception {
        SportField input = buildField(null, "Football Ground A", "Soccer", false, 800.0);
        SportField saved = buildField(1L,   "Football Ground A", "Soccer", false, 800.0);
        given(fieldService.addField(any())).willReturn(saved);

        mockMvc.perform(post("/api/fields")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Football Ground A"))
                .andExpect(jsonPath("$.indoor").value(false));
    }

    @Test
    @DisplayName("POST /api/fields returns 400 when name is blank")
    void createField_blankName_returns400() throws Exception {
        SportField invalid = buildField(null, "", "Soccer", false, 800.0);

        mockMvc.perform(post("/api/fields")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /api/fields returns 400 when pricePerHour is negative")
    void createField_negativePrice_returns400() throws Exception {
        SportField invalid = buildField(null, "Field A", "Soccer", false, -10.0);

        mockMvc.perform(post("/api/fields")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    // ── GET /api/fields ───────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/fields returns 200 with list of all fields")
    void getAllFields_returns200WithList() throws Exception {
        List<SportField> fields = List.of(
                buildField(1L, "Football Ground A", "Soccer",    false, 800.0),
                buildField(2L, "Badminton Court 1", "Badminton", true,  400.0)
        );
        given(fieldService.getAllFields()).willReturn(fields);

        mockMvc.perform(get("/api/fields"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Football Ground A"))
                .andExpect(jsonPath("$[1].indoor").value(true));
    }

    @Test
    @DisplayName("GET /api/fields returns empty array when no fields exist")
    void getAllFields_empty_returnsEmptyArray() throws Exception {
        given(fieldService.getAllFields()).willReturn(List.of());

        mockMvc.perform(get("/api/fields"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ── GET /api/fields/{id} ──────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/fields/{id} returns 200 with the field")
    void getFieldById_found_returns200() throws Exception {
        SportField field = buildField(1L, "Football Ground A", "Soccer", false, 800.0);
        given(fieldService.getFieldById(1L)).willReturn(field);

        mockMvc.perform(get("/api/fields/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Football Ground A"));
    }

    @Test
    @DisplayName("GET /api/fields/{id} returns 404 when field not found")
    void getFieldById_notFound_returns404() throws Exception {
        given(fieldService.getFieldById(99L)).willThrow(new FieldNotFoundException(99L));

        mockMvc.perform(get("/api/fields/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("FIELD_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Field not found with id: 99"));
    }

    // ── PUT /api/fields/{id} ──────────────────────────────────────────────

    @Test
    @DisplayName("PUT /api/fields/{id} returns 200 with updated field")
    void updateField_valid_returns200() throws Exception {
        SportField updated = buildField(1L, "New Name", "Cricket", true, 1000.0);
        given(fieldService.updateField(eq(1L), any())).willReturn(updated);

        mockMvc.perform(put("/api/fields/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Name"))
                .andExpect(jsonPath("$.indoor").value(true));
    }

    @Test
    @DisplayName("PUT /api/fields/{id} returns 404 when field not found")
    void updateField_notFound_returns404() throws Exception {
        given(fieldService.updateField(eq(99L), any())).willThrow(new FieldNotFoundException(99L));

        SportField details = buildField(null, "New Name", "Cricket", true, 1000.0);

        mockMvc.perform(put("/api/fields/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(details)))
                .andExpect(status().isNotFound());
    }

    // ── DELETE /api/fields/{id} ───────────────────────────────────────────

    @Test
    @DisplayName("DELETE /api/fields/{id} returns 200 with confirmation message")
    void deleteField_exists_returns200() throws Exception {
        willDoNothing().given(fieldService).deleteField(1L);

        mockMvc.perform(delete("/api/fields/1"))
                .andExpect(status().isOk())
                .andExpect(content().string("Field deleted successfully."));
    }

    @Test
    @DisplayName("DELETE /api/fields/{id} returns 404 when field not found")
    void deleteField_notFound_returns404() throws Exception {
        willThrow(new FieldNotFoundException(99L)).given(fieldService).deleteField(99L);

        mockMvc.perform(delete("/api/fields/99"))
                .andExpect(status().isNotFound());
    }
}
