package com.tomas.medical.appointment.mapper;

import com.tomas.medical.appointment.dto.response.AppointmentResponse;
import com.tomas.medical.appointment.entity.Appointment;
import org.springframework.stereotype.Component;

@Component
public class AppointmentMapper {

    public AppointmentResponse toResponse(Appointment appointment) {
        return toResponse(appointment, null, null);
    }

    public AppointmentResponse toResponse(Appointment appointment, String doctorFullName, String patientFullName) {
        return new AppointmentResponse(
                appointment.getId(),
                appointment.getDoctorId(),
                doctorFullName,
                appointment.getPatientId(),
                patientFullName,
                appointment.getAppointmentDate(),
                appointment.getStartTime(),
                appointment.getEndTime(),
                appointment.getStatus().name(),
                appointment.getCancellationReason(),
                appointment.getCancelledAt()
        );
    }
}
