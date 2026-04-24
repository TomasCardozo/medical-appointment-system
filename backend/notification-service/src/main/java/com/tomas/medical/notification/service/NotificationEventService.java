package com.tomas.medical.notification.service;

import com.tomas.medical.notification.dto.event.AppointmentCancelledEvent;
import com.tomas.medical.notification.dto.event.AppointmentCreatedEvent;
import com.tomas.medical.notification.dto.event.AppointmentReminderRequestedEvent;
import com.tomas.medical.notification.dto.event.AppointmentRescheduledEvent;
import com.tomas.medical.notification.entity.NotificationLog;
import com.tomas.medical.notification.repository.NotificationLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class NotificationEventService {

    public static final String APPOINTMENT_CREATED = "appointment.created";
    public static final String APPOINTMENT_CANCELLED = "appointment.cancelled";
    public static final String APPOINTMENT_RESCHEDULED = "appointment.rescheduled";
    public static final String APPOINTMENT_REMINDER_REQUESTED = "appointment.reminder.requested";

    private final NotificationLogRepository notificationLogRepository;
    private final NotificationSender notificationSender;

    public NotificationEventService(NotificationLogRepository notificationLogRepository,
                                    NotificationSender notificationSender) {
        this.notificationLogRepository = notificationLogRepository;
        this.notificationSender = notificationSender;
    }

    @Transactional
    public void handleAppointmentCreated(AppointmentCreatedEvent event) {
        process(
                event.appointmentId(),
                APPOINTMENT_CREATED,
                event.patientEmail(),
                "Turno confirmado",
                buildCreatedBody(event),
                event.occurredAt()
        );
    }

    @Transactional
    public void handleAppointmentCancelled(AppointmentCancelledEvent event) {
        process(
                event.appointmentId(),
                APPOINTMENT_CANCELLED,
                event.patientEmail(),
                "Turno cancelado",
                buildCancelledBody(event),
                event.occurredAt()
        );
    }

    @Transactional
    public void handleAppointmentRescheduled(AppointmentRescheduledEvent event) {
        process(
                event.appointmentId(),
                APPOINTMENT_RESCHEDULED,
                event.patientEmail(),
                "Turno reprogramado",
                buildRescheduledBody(event),
                event.occurredAt()
        );
    }

    @Transactional
    public void handleAppointmentReminderRequested(AppointmentReminderRequestedEvent event) {
        process(
                event.appointmentId(),
                APPOINTMENT_REMINDER_REQUESTED,
                event.patientEmail(),
                "Recordatorio de turno",
                buildReminderBody(event),
                event.occurredAt()
        );
    }

    private void process(Long appointmentId,
                         String eventType,
                         String recipientEmail,
                         String subject,
                         String body,
                         OffsetDateTime eventOccurredAt) {
        if (appointmentId == null) {
            return;
        }

        OffsetDateTime normalizedOccurredAt = eventOccurredAt != null ? eventOccurredAt : OffsetDateTime.now();

        if (recipientEmail != null && notificationLogRepository
                .existsByAppointmentIdAndEventTypeAndRecipientEmailAndEventOccurredAt(
                        appointmentId,
                        eventType,
                        recipientEmail,
                        normalizedOccurredAt
                )) {
            return;
        }

        NotificationLog logEntry = new NotificationLog();
        logEntry.setAppointmentId(appointmentId);
        logEntry.setEventType(eventType);
        logEntry.setRecipientEmail(recipientEmail);
        logEntry.setProvider(notificationSender.providerName());
        logEntry.setProcessedAt(OffsetDateTime.now());
        logEntry.setEventOccurredAt(normalizedOccurredAt);

        if (recipientEmail == null || recipientEmail.isBlank()) {
            logEntry.setStatus("SKIPPED");
            logEntry.setErrorMessage("Missing recipient email");
            notificationLogRepository.save(logEntry);
            return;
        }

        try {
            notificationSender.send(new NotificationMessage(recipientEmail, subject, body));
            logEntry.setStatus("SENT");
            notificationLogRepository.save(logEntry);
        } catch (RuntimeException ex) {
            logEntry.setStatus("FAILED");
            logEntry.setErrorMessage(ex.getMessage());
            notificationLogRepository.save(logEntry);
            throw ex;
        }
    }

    private String buildCreatedBody(AppointmentCreatedEvent event) {
        return "Hola " + fallback(event.patientFullName())
               + ", tu turno fue confirmado para " + event.appointmentDate()
               + " a las " + event.startTime() + ".";
    }

    private String buildCancelledBody(AppointmentCancelledEvent event) {
        String reason = event.cancellationReason() == null ? "sin detalle" : event.cancellationReason();
        return "Hola " + fallback(event.patientFullName())
               + ", tu turno del " + event.appointmentDate()
               + " a las " + event.startTime() + " fue cancelado. Motivo: " + reason + ".";
    }

    private String buildRescheduledBody(AppointmentRescheduledEvent event) {
        String reason = event.rescheduleReason() == null ? "sin detalle" : event.rescheduleReason();
        return "Hola " + fallback(event.patientFullName())
               + ", tu turno del " + event.previousAppointmentDate()
               + " a las " + event.previousStartTime()
               + " fue reprogramado para " + event.appointmentDate()
               + " a las " + event.startTime()
               + ". Motivo: " + reason + ".";
    }

    private String buildReminderBody(AppointmentReminderRequestedEvent event) {
        return "Hola " + fallback(event.patientFullName())
               + ", te recordamos tu turno para " + event.appointmentDate()
               + " a las " + event.startTime() + ".";
    }

    private String fallback(String value) {
        return value == null || value.isBlank() ? "paciente" : value;
    }
}
