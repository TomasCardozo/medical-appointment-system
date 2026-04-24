package com.tomas.medical.appointment.client.dto;

public record InternalDoctorOwnerResponse(
        Long id,
        String ownerEmail,
        boolean active
) {
}
