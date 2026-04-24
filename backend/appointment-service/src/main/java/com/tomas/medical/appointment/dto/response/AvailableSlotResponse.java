package com.tomas.medical.appointment.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;

public record AvailableSlotResponse(
        Long doctorId,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        Integer slotDurationMinutes
) {
}
