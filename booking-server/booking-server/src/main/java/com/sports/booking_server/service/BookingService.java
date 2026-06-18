package com.sports.booking_server.service;

import com.sports.booking_server.client.FieldClient;
import com.sports.booking_server.client.WeatherClient;
import com.sports.booking_server.dto.FieldDTO;
import com.sports.booking_server.entity.Booking;
import com.sports.booking_server.exception.FieldAlreadyBookedException;
import com.sports.booking_server.exception.WeatherConditionException;
import com.sports.booking_server.repository.BookingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);


    private static final double DEFAULT_LAT = 12.97;
    private static final double DEFAULT_LON = 77.59;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private FieldClient fieldClient; 

    @Autowired
    private WeatherClient weatherClient; 
    
//    -------------------------------------------

    public Booking createBooking(Booking booking) {
        log.info("Processing booking: fieldId={}, date={}, hour={}, client='{}'",
                booking.getFieldId(), booking.getBookingDate(), booking.getStartHour(), booking.getClientName());

        // Step 1: Availability check 
        boolean alreadyBooked = bookingRepository.existsByFieldIdAndBookingDateAndStartHour(
                booking.getFieldId(), booking.getBookingDate(), booking.getStartHour());

        if (alreadyBooked) {
            log.warn("Booking conflict: fieldId={}, date={}, hour={}",
                    booking.getFieldId(), booking.getBookingDate(), booking.getStartHour());
            throw new FieldAlreadyBookedException(
                    booking.getFieldId(),
                    booking.getBookingDate().toString(),
                    booking.getStartHour()
            );
        }
        log.debug("Step 1 passed: slot is available");

        // Step 2: Fetch field details from FIELD-SERVICE
        log.info("Fetching field details for fieldId={} from FIELD-SERVICE", booking.getFieldId());
        FieldDTO field = fieldClient.getFieldById(booking.getFieldId());
        log.info("Field fetched: name='{}', type='{}', indoor={}", field.getName(), field.getType(), field.isIndoor());

        // Step 3: Weather suitability check (outdoor fields only)
	     boolean shouldCheckWeather =
	             !field.getType().equalsIgnoreCase("Football");
	
	     if (shouldCheckWeather) {
	         log.info("Weather validation required for sport type='{}'",
	                 field.getType());
	         boolean suitable =
	                 weatherClient.isWeatherSuitable(
	                         DEFAULT_LAT,
	                         DEFAULT_LON
	                 );
	
	         if (!suitable) {
	             log.warn(
	                 "Weather check FAILED for field id={}",
	                 booking.getFieldId()
	             );
	             throw new WeatherConditionException(-1.0);
	         }
	         log.debug("Step 3 passed: weather is GOOD");
	     } else {
	         log.debug(
	             "Weather check skipped for Football field"
	         );
	     }
     
     
        // Limiting the date
        int startHour = booking.getStartHour();

        if (startHour < 6 || startHour > 22) {
            throw new RuntimeException(
                    "Booking allowed only between 6 AM and 10 PM");
        }

        LocalDate bookingDate = booking.getBookingDate();
        LocalDate maxDate = LocalDate.now().plusDays(3);

        if (bookingDate.isAfter(maxDate)) {
            throw new RuntimeException(
                    "Bookings allowed only up to 3 days in advance");
        }

        // Step 4: Persist the booking
        Booking saved = bookingRepository.save(booking);
        log.info("Booking confirmed: id={}, fieldId={}, client='{}'",
                saved.getId(), saved.getFieldId(), saved.getClientName());
        return saved;
    }
    
//    -------------------------------------------

    public List<Booking> getReservationsByDate(Long fieldId, LocalDate date) {
        log.info("Fetching reservations for fieldId={}, date={}", fieldId, date);
        List<Booking> reservations = bookingRepository.findByFieldIdAndBookingDate(fieldId, date);
        log.info("Found {} reservation(s) for fieldId={} on {}", reservations.size(), fieldId, date);
        return reservations;
    }
    
//    -------------------------------------------
    
    public int deleteBookingsByFieldId(Long fieldId) {
        log.info("Deleting all bookings for fieldId={}", fieldId);
        int count = bookingRepository.deleteByFieldId(fieldId);
        log.info("Deleted {} booking(s) for fieldId={}", count, fieldId);
        return count;
    }
}
