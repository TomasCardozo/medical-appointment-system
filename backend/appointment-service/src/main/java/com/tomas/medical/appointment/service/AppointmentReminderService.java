package com.tomas.medical.appointment.service;

import com.tomas.medical.appointment.client.AuthServiceClient;
import com.tomas.medical.appointment.client.DoctorServiceClient;
import com.tomas.medical.appointment.client.dto.InternalDoctorSummaryResponse;
import com.tomas.medical.appointment.client.dto.InternalUserResponse;
import com.tomas.medical.appointment.dto.event.AppointmentReminderRequestedEvent;
import com.tomas.medical.appointment.entity.Appointment;
import com.tomas.medical.appointment.enumtype.AppointmentStatus;
import com.tomas.medical.appointment.messaging.producer.AppointmentEventPublisher;
import com.tomas.medical.appointment.repository.AppointmentRepository;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Service
public class AppointmentReminderService {

    private static final Logger log = LoggerFactory.getLogger(AppointmentReminderService.class);

    private final AppointmentRepository appointmentRepository;
    private final AppointmentEventPublisher appointmentEventPublisher;
    private final AuthServiceClient authServiceClient;
    private final DoctorServiceClient doctorServiceClient;
    private final long reminderWindowHours;

    public AppointmentReminderService(AppointmentRepository appointmentRepository,
                                      AppointmentEventPublisher appointmentEventPublisher,
                                      AuthServiceClient authServiceClient,
                                      DoctorServiceClient doctorServiceClient,
                                      @Value("${app.reminders.window-hours:24}") long reminderWindowHours) {
        this.appointmentRepository = appointmentRepository;
        this.appointmentEventPublisher = appointmentEventPublisher;
        this.authServiceClient = authServiceClient;
        this.doctorServiceClient = doctorServiceClient;
        this.reminderWindowHours = reminderWindowHours;
    }

    @Scheduled(fixedDelayString = "${app.reminders.scan-interval-ms:60000}")
    @Transactional
    public void scanUpcomingAppointmentsForReminders() {
        int processed = dispatchReminderRequests();
        if (processed > 0) {
            log.info("Published {} appointment.reminder.requested events", processed);
        }
    }

    @Transactional
    int dispatchReminderRequests() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowEnd = now.plusHours(reminderWindowHours);
        LocalDate from = now.toLocalDate();
        LocalDate to = windowEnd.toLocalDate();

        int published = 0;

        for (Appointment appointment : appointmentRepository
                .findAllByStatusAndReminderRequestedAtIsNullAndAppointmentDateBetweenOrderByAppointmentDateAscStartTimeAsc(
                        AppointmentStatus.BOOKED,
                        from,
                        to
                )) {
            if (appointment.getReminderRequestedAt() != null) {
                continue;
            }

            LocalDateTime slotDateTime = LocalDateTime.of(appointment.getAppointmentDate(), appointment.getStartTime());
            if (slotDateTime.isBefore(now) || slotDateTime.isAfter(windowEnd)) {
                continue;
            }

            OffsetDateTime reminderRequestedAt = OffsetDateTime.now();
            InternalUserResponse patient = safeResolveUserById(appointment.getPatientId());

            AppointmentReminderRequestedEvent event = new AppointmentReminderRequestedEvent(
                    appointment.getId(),
                    appointment.getDoctorId(),
                    safeResolveDoctorNameById(appointment.getDoctorId()),
                    appointment.getPatientId(),
                    patient != null ? patient.fullName() : null,
                    patient != null ? patient.email() : null,
                    appointment.getAppointmentDate(),
                    appointment.getStartTime(),
                    appointment.getEndTime(),
                    reminderRequestedAt,
                    OffsetDateTime.now()
            );

            try {
                appointmentEventPublisher.publishReminderRequested(event);
                appointment.setReminderRequestedAt(reminderRequestedAt);
                published++;
            } catch (RuntimeException ex) {
                log.warn("Failed to publish appointment.reminder.requested for appointmentId={}", appointment.getId(), ex);
            }
        }

        return published;
    }

    private InternalUserResponse safeResolveUserById(Long userId) {
        try {
            return authServiceClient.getUserById(userId);
        } catch (FeignException ex) {
            log.warn("Could not resolve patient identity for reminder. userId={}", userId, ex);
            return null;
        }
    }

    private String safeResolveDoctorNameById(Long doctorId) {
        try {
            InternalDoctorSummaryResponse doctor = doctorServiceClient.getDoctorById(doctorId);
            return doctor != null ? doctor.fullName() : null;
        } catch (FeignException ex) {
            log.warn("Could not resolve doctor identity for reminder. doctorId={}", doctorId, ex);
            return null;
        }
    }
}
