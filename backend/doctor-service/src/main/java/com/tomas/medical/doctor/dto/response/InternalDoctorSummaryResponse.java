package com.tomas.medical.doctor.dto.response;

public record InternalDoctorSummaryResponse(
        Long id,
        String fullName,
        boolean active
) {
}
