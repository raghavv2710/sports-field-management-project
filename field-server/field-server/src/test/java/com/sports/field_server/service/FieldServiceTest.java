package com.sports.field_server.service;

import com.sports.field_server.entity.SportField;
import com.sports.field_server.exception.FieldNotFoundException;
import com.sports.field_server.repository.FieldRepository;
import com.sports.field_server.service.FieldService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class FieldServiceTest {

    @Mock
    private FieldRepository fieldRepository;

    @InjectMocks
    private FieldService fieldService;

    // ── helper ────────────────────────────────────────────────────────────
    private SportField buildField(Long id, String name, String type, boolean indoor, double price) {
        return new SportField(id, name, type, indoor, price);
    }

    // ── addField ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("addField saves and returns the field with generated ID")
    void addField_validField_returnsSavedField() {
        SportField input  = buildField(null, "Football Ground A", "Soccer", false, 800.0);
        SportField saved  = buildField(1L,   "Football Ground A", "Soccer", false, 800.0);
        given(fieldRepository.save(input)).willReturn(saved);

        SportField result = fieldService.addField(input);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Football Ground A");
        then(fieldRepository).should(times(1)).save(input);
    }

    // ── getAllFields ──────────────────────────────────────────────────────

    @Test
    @DisplayName("getAllFields returns all fields from repository")
    void getAllFields_returnsAllFields() {
        List<SportField> fields = List.of(
                buildField(1L, "Field A", "Soccer",    false, 800.0),
                buildField(2L, "Court B", "Badminton", true,  400.0)
        );
        given(fieldRepository.findAll()).willReturn(fields);

        List<SportField> result = fieldService.getAllFields();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(SportField::getName)
                .containsExactly("Field A", "Court B");
    }

    @Test
    @DisplayName("getAllFields returns empty list when no fields exist")
    void getAllFields_noFields_returnsEmptyList() {
        given(fieldRepository.findAll()).willReturn(List.of());

        assertThat(fieldService.getAllFields()).isEmpty();
    }

    // ── getFieldById ──────────────────────────────────────────────────────

    @Test
    @DisplayName("getFieldById returns field when it exists")
    void getFieldById_exists_returnsField() {
        SportField field = buildField(1L, "Football Ground A", "Soccer", false, 800.0);
        given(fieldRepository.findById(1L)).willReturn(Optional.of(field));

        SportField result = fieldService.getFieldById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Football Ground A");
    }

    @Test
    @DisplayName("getFieldById throws FieldNotFoundException when field does not exist")
    void getFieldById_notFound_throwsFieldNotFoundException() {
        given(fieldRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> fieldService.getFieldById(99L))
                .isInstanceOf(FieldNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ── updateField ───────────────────────────────────────────────────────

    @Test
    @DisplayName("updateField updates all mutable fields and returns updated entity")
    void updateField_exists_updatesAndReturns() {
        SportField existing = buildField(1L, "Old Name", "Soccer", false, 500.0);
        SportField details  = buildField(null, "New Name", "Cricket", true,  900.0);
        SportField updated  = buildField(1L, "New Name", "Cricket", true,  900.0);

        given(fieldRepository.findById(1L)).willReturn(Optional.of(existing));
        given(fieldRepository.save(any())).willReturn(updated);

        SportField result = fieldService.updateField(1L, details);

        assertThat(result.getName()).isEqualTo("New Name");
        assertThat(result.getType()).isEqualTo("Cricket");
        assertThat(result.isIndoor()).isTrue();
        assertThat(result.getPricePerHour()).isEqualTo(900.0);
    }

    @Test
    @DisplayName("updateField throws FieldNotFoundException when field does not exist")
    void updateField_notFound_throwsFieldNotFoundException() {
        given(fieldRepository.findById(42L)).willReturn(Optional.empty());

        SportField details = buildField(null, "X", "Y", false, 100.0);

        assertThatThrownBy(() -> fieldService.updateField(42L, details))
                .isInstanceOf(FieldNotFoundException.class);
    }

    @Test
    @DisplayName("updateField does NOT change the entity's ID")
    void updateField_doesNotChangeId() {
        SportField existing = buildField(5L, "Old", "Soccer", false, 600.0);
        given(fieldRepository.findById(5L)).willReturn(Optional.of(existing));
        given(fieldRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        SportField details = buildField(99L, "New", "Cricket", true, 999.0);
        SportField result  = fieldService.updateField(5L, details);

        // ID must remain 5, not 99
        assertThat(result.getId()).isEqualTo(5L);
    }

    // ── deleteField ───────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteField calls deleteById when field exists")
    void deleteField_exists_callsDeleteById() {
        SportField field = buildField(1L, "X", "Y", false, 100.0);
        given(fieldRepository.findById(1L)).willReturn(Optional.of(field));

        fieldService.deleteField(1L);

        then(fieldRepository).should().deleteById(1L);
    }

    @Test
    @DisplayName("deleteField throws FieldNotFoundException when field does not exist")
    void deleteField_notFound_throwsFieldNotFoundException() {
        given(fieldRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> fieldService.deleteField(99L))
                .isInstanceOf(FieldNotFoundException.class);

        // deleteById must NOT be called if the field doesn't exist
        then(fieldRepository).should(never()).deleteById(anyLong());
    }
}
