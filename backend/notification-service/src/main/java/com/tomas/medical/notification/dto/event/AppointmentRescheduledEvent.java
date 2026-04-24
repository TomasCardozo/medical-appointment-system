package com.tomas.medical.notification.dto.event;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

public record AppointmentRescheduledEvent(
        Long appointmentId,
        Long doctorId,
        String doctorFullName,
        Long patientId,
        String patientFullName,
        String patientEmail,
        LocalDate previousAppointmentDate,
        LocalTime previousStartTime,
        LocalTime previousEndTime,
        LocalDate appointmentDate,
        LocalTime startTime,
        LocalTime endTime,
        String rescheduleReason,
        OffsetDateTime rescheduledAt,
        OffsetDateTime occurredAt
) {
}
