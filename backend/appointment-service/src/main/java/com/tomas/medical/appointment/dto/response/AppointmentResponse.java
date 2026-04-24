package com.tomas.medical.appointment.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

public record AppointmentResponse(
        Long id,
        Long doctorId,
        String doctorFullName,
        Long patientId,
        String patientFullName,
        LocalDate appointmentDate,
        LocalTime startTime,
        LocalTime endTime,
        String status,
        String cancellationReason,
        OffsetDateTime cancelledAt
) {
}
