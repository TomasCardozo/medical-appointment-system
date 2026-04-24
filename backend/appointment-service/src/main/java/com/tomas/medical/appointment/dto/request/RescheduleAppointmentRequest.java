package com.tomas.medical.appointment.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public record RescheduleAppointmentRequest(
        @NotNull(message = "appointmentDate is required")
        LocalDate appointmentDate,

        @NotNull(message = "startTime is required")
        LocalTime startTime,

        @Size(max = 400, message = "rescheduleReason cannot exceed 400 characters")
        String rescheduleReason
) {
}
