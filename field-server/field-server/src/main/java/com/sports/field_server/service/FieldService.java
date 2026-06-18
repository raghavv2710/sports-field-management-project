package com.sports.field_server.service;

import com.sports.field_server.client.BookingClient;
import com.sports.field_server.entity.SportField;
import com.sports.field_server.exception.FieldNotFoundException;
import com.sports.field_server.repository.FieldRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class FieldService {

    private static final Logger log = LoggerFactory.getLogger(FieldService.class);

    @Autowired
    private FieldRepository fieldRepository;
    
    @Autowired
    private BookingClient bookingClient;

    public SportField addField(SportField field) {
        log.debug("Saving new SportField: {}", field.getName());
        SportField saved = fieldRepository.save(field);
        log.debug("SportField saved with id={}", saved.getId());
        return saved;
    }


    public List<SportField> getAllFields() {
        log.debug("Querying all sport fields");
        return fieldRepository.findAll();
    }


    public SportField getFieldById(Long id) {
        log.debug("Querying SportField id={}", id);
        return fieldRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("SportField id={} not found", id);
                    return new FieldNotFoundException(id);
                });
    }


    public SportField updateField(Long id, SportField fieldDetails) {
        log.debug("Updating SportField id={}", id);
        SportField field = getFieldById(id); // Throws FieldNotFoundException if absent

        field.setName(fieldDetails.getName());
        field.setType(fieldDetails.getType());
        field.setIndoor(fieldDetails.isIndoor());
        field.setPricePerHour(fieldDetails.getPricePerHour());

        SportField updated = fieldRepository.save(field);
        log.debug("SportField id={} updated: {}", id, updated);
        return updated;
    }

    public void deleteField(Long id) {
        log.debug("Deleting SportField id={}", id);

        // Step 1: verify field exists first — fail fast if it doesn't
        getFieldById(id);

        // Step 2: delete all bookings for this field from booking-server
        // This is a direct service-to-service Feign call via Eureka (not through gateway)
        log.info("Deleting all bookings for fieldId={} via BOOKING-SERVER", id);
        try {
            bookingClient.deleteBookingsByFieldId(id);
            log.info("All bookings for fieldId={} deleted successfully", id);
        } catch (Exception e) {
            // booking-server is down or returned an error
            log.error("Failed to delete bookings for fieldId={}: {}", id, e.getMessage());
            throw new RuntimeException(
                "Could not delete bookings for field " + id +
                ". BOOKING-SERVER may be unavailable. " +
                "Field was NOT deleted to maintain consistency. Please retry."
            );
        }

        // Step 3: delete the field itself
        fieldRepository.deleteById(id);
        log.info("SportField id={} and all its bookings deleted successfully", id);
    }
}
