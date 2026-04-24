package com.tomas.medical.notification.dto.event;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

public record AppointmentCreatedEvent(
        Long appointmentId,
        Long doctorId,
        String doctorFullName,
        Long patientId,
        String patientFullName,
        String patientEmail,
        LocalDate appointmentDate,
        LocalTime startTime,
        LocalTime endTime,
        OffsetDateTime occurredAt
) {
}
