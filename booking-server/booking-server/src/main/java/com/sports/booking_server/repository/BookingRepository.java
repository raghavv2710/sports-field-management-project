package com.sports.booking_server.repository;

import com.sports.booking_server.entity.Booking;

import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    boolean existsByFieldIdAndBookingDateAndStartHour(
            Long fieldId, LocalDate bookingDate, int startHour);

    List<Booking> findByFieldIdAndBookingDate(Long fieldId, LocalDate bookingDate);
    
    @Modifying
    @Transactional
    int deleteByFieldId(Long fieldId);
}