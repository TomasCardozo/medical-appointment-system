package com.tomas.medical.doctor.exception;

public class DoctorProfileAlreadyExistsException extends RuntimeException {

    public DoctorProfileAlreadyExistsException(String ownerEmail) {
        super("Doctor profile already exists for owner email: " + ownerEmail);
    }
}
