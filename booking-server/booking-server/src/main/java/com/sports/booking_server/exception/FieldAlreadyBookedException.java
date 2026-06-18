package com.sports.booking_server.exception;


public class FieldAlreadyBookedException extends RuntimeException {

    public FieldAlreadyBookedException(Long fieldId, String date, int startHour) {
        super(String.format(
            "Field %d is already booked on %s at %02d:00. Please choose a different time slot.",
            fieldId, date, startHour
        ));
    }
}
