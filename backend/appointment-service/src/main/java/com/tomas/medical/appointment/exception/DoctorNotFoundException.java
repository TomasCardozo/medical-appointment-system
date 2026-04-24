package com.tomas.medical.appointment.exception;

public class DoctorNotFoundException extends RuntimeException {

    public DoctorNotFoundException(Long doctorId) {
        super("Doctor not found for id: " + doctorId);
    }
}
