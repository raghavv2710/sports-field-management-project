package com.sports.field_server.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;


//Feign client for calling BOOKING-SERVER from field-server

@FeignClient(name = "BOOKING-SERVER")
public interface BookingClient {

    @DeleteMapping("/api/bookings/field/{fieldId}")
    void deleteBookingsByFieldId(@PathVariable("fieldId") Long fieldId);
}