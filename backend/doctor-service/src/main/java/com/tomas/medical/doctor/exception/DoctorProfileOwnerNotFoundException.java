package com.tomas.medical.doctor.exception;

public class DoctorProfileOwnerNotFoundException extends RuntimeException {

    public DoctorProfileOwnerNotFoundException(String ownerEmail) {
        super("Doctor profile not found for owner email: " + ownerEmail);
    }
}
