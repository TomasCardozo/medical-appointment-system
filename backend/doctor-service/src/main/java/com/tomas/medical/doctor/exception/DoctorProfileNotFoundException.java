package com.tomas.medical.doctor.exception;

public class DoctorProfileNotFoundException extends RuntimeException {

    public DoctorProfileNotFoundException(Long doctorId) {
        super("Doctor profile not found for id: " + doctorId);
    }
}
