package com.tomas.medical.doctor.exception;

public class DoctorAvailabilityConflictException extends RuntimeException {

    public DoctorAvailabilityConflictException(String message) {
        super(message);
    }
}
