package com.tomas.medical.doctor.exception;

public class DoctorAvailabilityNotFoundException extends RuntimeException {

    public DoctorAvailabilityNotFoundException(Long availabilityId) {
        super("Doctor availability not found for id: " + availabilityId);
    }
}
