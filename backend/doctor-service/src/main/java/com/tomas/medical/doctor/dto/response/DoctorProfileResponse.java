package com.tomas.medical.doctor.dto.response;

public record DoctorProfileResponse(
        Long id,
        String fullName,
        String specialty,
        String licenseNumber,
        String clinicAddress,
        String bio
) {
}
