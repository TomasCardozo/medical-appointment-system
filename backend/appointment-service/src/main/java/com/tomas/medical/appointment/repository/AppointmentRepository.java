package com.tomas.medical.appointment.repository;

import com.tomas.medical.appointment.entity.Appointment;
import com.tomas.medical.appointment.enumtype.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    boolean existsByDoctorIdAndAppointmentDateAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
            Long doctorId,
            LocalDate appointmentDate,
            AppointmentStatus status,
            LocalTime endTime,
            LocalTime startTime
    );

    boolean existsByDoctorIdAndAppointmentDateAndStatusAndStartTimeLessThanAndEndTimeGreaterThanAndIdNot(
            Long doctorId,
            LocalDate appointmentDate,
            AppointmentStatus status,
            LocalTime endTime,
            LocalTime startTime,
            Long appointmentId
    );

    List<Appointment> findAllByDoctorIdAndAppointmentDateAndStatusOrderByStartTimeAsc(
            Long doctorId,
            LocalDate appointmentDate,
            AppointmentStatus status
    );

    List<Appointment> findAllByPatientIdOrderByAppointmentDateDescStartTimeDesc(Long patientId);

    List<Appointment> findAllByDoctorIdOrderByAppointmentDateDescStartTimeDesc(Long doctorId);

    List<Appointment> findAllByDoctorIdAndAppointmentDateBetweenOrderByAppointmentDateAscStartTimeAsc(
            Long doctorId,
            LocalDate from,
            LocalDate to
    );

    List<Appointment> findAllByStatusAndReminderRequestedAtIsNullAndAppointmentDateBetweenOrderByAppointmentDateAscStartTimeAsc(
            AppointmentStatus status,
            LocalDate from,
            LocalDate to
    );
}
