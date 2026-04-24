package com.tomas.medical.doctor.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;

public record BlockedSlotResponse(
        Long id,
        LocalDate blockedDate,
        LocalTime startTime,
        LocalTime endTime
) {
}
