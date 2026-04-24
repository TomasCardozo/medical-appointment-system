package com.tomas.medical.doctor.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public record CreateBlockedSlotRequest(
        @NotNull(message = "blockedDate is required")
        LocalDate blockedDate,

        @NotNull(message = "startTime is required")
        LocalTime startTime,

        @NotNull(message = "endTime is required")
        LocalTime endTime,

        @Size(max = 200, message = "reason must be at most 200 characters")
        String reason
) {
}
