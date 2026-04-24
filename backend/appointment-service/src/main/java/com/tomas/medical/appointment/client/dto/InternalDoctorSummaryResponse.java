package com.tomas.medical.appointment.client.dto;

public record InternalDoctorSummaryResponse(
        Long id,
        String fullName,
        boolean active
) {
}
