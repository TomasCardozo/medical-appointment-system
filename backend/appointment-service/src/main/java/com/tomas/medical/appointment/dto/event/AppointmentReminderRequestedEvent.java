package com.tomas.medical.appointment.dto.event;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

public record AppointmentReminderRequestedEvent(
        Long appointmentId,
        Long doctorId,
        String doctorFullName,
        Long patientId,
        String patientFullName,
        String patientEmail,
        LocalDate appointmentDate,
        LocalTime startTime,
        LocalTime endTime,
        OffsetDateTime reminderRequestedAt,
        OffsetDateTime occurredAt
) {
}
