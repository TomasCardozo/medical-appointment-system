package com.tomas.medical.doctor.dto.response;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record DoctorScheduleResponse(
        Long doctorId,
        LocalDate from,
        LocalDate to,
        List<DoctorAvailabilityResponse> availabilityRules,
        List<DoctorScheduleDayResponse> days
) {

    public record DoctorScheduleDayResponse(
            LocalDate date,
            DayOfWeek dayOfWeek,
            List<DoctorScheduleWindowResponse> availableWindows,
            List<DoctorScheduleBlockedSlotResponse> blockedSlots
    ) {
    }

    public record DoctorScheduleWindowResponse(
            Long availabilityId,
            LocalTime startTime,
            LocalTime endTime,
            Integer slotDurationMinutes
    ) {
    }

    public record DoctorScheduleBlockedSlotResponse(
            Long id,
            LocalTime startTime,
            LocalTime endTime
    ) {
    }
}
