package com.tomas.medical.appointment.dto.request;

import jakarta.validation.constraints.Size;

public record CancelAppointmentRequest(
        @Size(max = 400, message = "cancellationReason cannot exceed 400 characters")
        String cancellationReason
) {
}
