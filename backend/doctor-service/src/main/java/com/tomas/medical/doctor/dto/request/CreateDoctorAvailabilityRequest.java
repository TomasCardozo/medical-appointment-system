package com.tomas.medical.doctor.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record CreateDoctorAvailabilityRequest(
        @NotNull(message = "dayOfWeek is required")
        DayOfWeek dayOfWeek,

        @NotNull(message = "startTime is required")
        LocalTime startTime,

        @NotNull(message = "endTime is required")
        LocalTime endTime,

        @NotNull(message = "slotDurationMinutes is required")
        @Min(value = 15, message = "slotDurationMinutes must be at least 15")
        @Max(value = 240, message = "slotDurationMinutes must be at most 240")
        Integer slotDurationMinutes
) {
}
