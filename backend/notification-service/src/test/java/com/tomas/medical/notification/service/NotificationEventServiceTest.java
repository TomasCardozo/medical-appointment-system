package com.tomas.medical.notification.service;

import com.tomas.medical.notification.dto.event.AppointmentCancelledEvent;
import com.tomas.medical.notification.dto.event.AppointmentCreatedEvent;
import com.tomas.medical.notification.dto.event.AppointmentReminderRequestedEvent;
import com.tomas.medical.notification.dto.event.AppointmentRescheduledEvent;
import com.tomas.medical.notification.repository.NotificationLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationEventServiceTest {

    @Mock
    private NotificationLogRepository notificationLogRepository;

    @Mock
    private NotificationSender notificationSender;

    private NotificationEventService notificationEventService;

    @BeforeEach
    void setUp() {
        notificationEventService = new NotificationEventService(notificationLogRepository, notificationSender);
    }

    @Test
    void createdEventSendsNotificationAndPersistsLog() {
        OffsetDateTime occurredAt = OffsetDateTime.now().minusMinutes(2);
        when(notificationSender.providerName()).thenReturn("log");
        when(notificationLogRepository.existsByAppointmentIdAndEventTypeAndRecipientEmailAndEventOccurredAt(
                11L,
                NotificationEventService.APPOINTMENT_CREATED,
                "patient@example.com",
                occurredAt
        )).thenReturn(false);

        notificationEventService.handleAppointmentCreated(new AppointmentCreatedEvent(
                11L,
                7L,
                "Dr. Gomez",
                100L,
                "Ana",
                "patient@example.com",
                LocalDate.of(2026, 5, 10),
                LocalTime.of(9, 0),
                LocalTime.of(9, 30),
                occurredAt
        ));

        verify(notificationSender).send(any(NotificationMessage.class));
        verify(notificationLogRepository).save(any());
    }

    @Test
    void cancelledEventSkipsWhenAlreadyProcessed() {
        OffsetDateTime occurredAt = OffsetDateTime.now().minusMinutes(1);
        when(notificationLogRepository.existsByAppointmentIdAndEventTypeAndRecipientEmailAndEventOccurredAt(
                12L,
                NotificationEventService.APPOINTMENT_CANCELLED,
                "patient@example.com",
                occurredAt
        )).thenReturn(true);

        notificationEventService.handleAppointmentCancelled(new AppointmentCancelledEvent(
                12L,
                7L,
                "Dr. Gomez",
                100L,
                "Ana",
                "patient@example.com",
                LocalDate.of(2026, 5, 10),
                LocalTime.of(9, 0),
                LocalTime.of(9, 30),
                "No puede asistir",
                OffsetDateTime.now(),
                occurredAt
        ));

        verify(notificationSender, never()).send(any(NotificationMessage.class));
        verify(notificationLogRepository, never()).save(any());
    }

    @Test
    void rescheduledEventSendsNotificationAndPersistsLog() {
        OffsetDateTime occurredAt = OffsetDateTime.now();
        when(notificationSender.providerName()).thenReturn("log");
        when(notificationLogRepository.existsByAppointmentIdAndEventTypeAndRecipientEmailAndEventOccurredAt(
                13L,
                NotificationEventService.APPOINTMENT_RESCHEDULED,
                "patient@example.com",
                occurredAt
        )).thenReturn(false);

        notificationEventService.handleAppointmentRescheduled(new AppointmentRescheduledEvent(
                13L,
                7L,
                "Dr. Gomez",
                100L,
                "Ana",
                "patient@example.com",
                LocalDate.of(2026, 5, 10),
                LocalTime.of(9, 0),
                LocalTime.of(9, 30),
                LocalDate.of(2026, 5, 11),
                LocalTime.of(11, 0),
                LocalTime.of(11, 30),
                "Cambio de agenda",
                OffsetDateTime.now().minusMinutes(5),
                occurredAt
        ));

        verify(notificationSender).send(any(NotificationMessage.class));
        verify(notificationLogRepository).save(any());
    }

    @Test
    void rescheduledEventSkipsWhenSameOccurredAtWasAlreadyProcessed() {
        OffsetDateTime occurredAt = OffsetDateTime.now();
        when(notificationLogRepository.existsByAppointmentIdAndEventTypeAndRecipientEmailAndEventOccurredAt(
                14L,
                NotificationEventService.APPOINTMENT_RESCHEDULED,
                "patient@example.com",
                occurredAt
        )).thenReturn(true);

        notificationEventService.handleAppointmentRescheduled(new AppointmentRescheduledEvent(
                14L,
                7L,
                "Dr. Gomez",
                100L,
                "Ana",
                "patient@example.com",
                LocalDate.of(2026, 5, 10),
                LocalTime.of(9, 0),
                LocalTime.of(9, 30),
                LocalDate.of(2026, 5, 11),
                LocalTime.of(11, 0),
                LocalTime.of(11, 30),
                null,
                OffsetDateTime.now().minusMinutes(5),
                occurredAt
        ));

        verify(notificationSender, never()).send(any(NotificationMessage.class));
        verify(notificationLogRepository, never()).save(any());
    }

    @Test
    void rescheduledEventSendsAgainWhenOccurredAtDiffers() {
        OffsetDateTime firstOccurredAt = OffsetDateTime.now().minusMinutes(2);
        OffsetDateTime secondOccurredAt = OffsetDateTime.now().minusMinutes(1);

        when(notificationSender.providerName()).thenReturn("log");
        when(notificationLogRepository.existsByAppointmentIdAndEventTypeAndRecipientEmailAndEventOccurredAt(
                15L,
                NotificationEventService.APPOINTMENT_RESCHEDULED,
                "patient@example.com",
                firstOccurredAt
        )).thenReturn(false);
        when(notificationLogRepository.existsByAppointmentIdAndEventTypeAndRecipientEmailAndEventOccurredAt(
                15L,
                NotificationEventService.APPOINTMENT_RESCHEDULED,
                "patient@example.com",
                secondOccurredAt
        )).thenReturn(false);

        notificationEventService.handleAppointmentRescheduled(new AppointmentRescheduledEvent(
                15L,
                7L,
                "Dr. Gomez",
                100L,
                "Ana",
                "patient@example.com",
                LocalDate.of(2026, 5, 10),
                LocalTime.of(9, 0),
                LocalTime.of(9, 30),
                LocalDate.of(2026, 5, 11),
                LocalTime.of(11, 0),
                LocalTime.of(11, 30),
                null,
                OffsetDateTime.now().minusMinutes(5),
                firstOccurredAt
        ));

        notificationEventService.handleAppointmentRescheduled(new AppointmentRescheduledEvent(
                15L,
                7L,
                "Dr. Gomez",
                100L,
                "Ana",
                "patient@example.com",
                LocalDate.of(2026, 5, 11),
                LocalTime.of(11, 0),
                LocalTime.of(11, 30),
                LocalDate.of(2026, 5, 12),
                LocalTime.of(13, 0),
                LocalTime.of(13, 30),
                null,
                OffsetDateTime.now().minusMinutes(1),
                secondOccurredAt
        ));

        verify(notificationSender, times(2)).send(any(NotificationMessage.class));
        verify(notificationLogRepository, times(2)).save(any());
    }

    @Test
    void reminderRequestedEventSendsNotificationAndPersistsLog() {
        OffsetDateTime occurredAt = OffsetDateTime.now();
        when(notificationSender.providerName()).thenReturn("log");
        when(notificationLogRepository.existsByAppointmentIdAndEventTypeAndRecipientEmailAndEventOccurredAt(
                16L,
                NotificationEventService.APPOINTMENT_REMINDER_REQUESTED,
                "patient@example.com",
                occurredAt
        )).thenReturn(false);

        notificationEventService.handleAppointmentReminderRequested(new AppointmentReminderRequestedEvent(
                16L,
                7L,
                "Dr. Gomez",
                100L,
                "Ana",
                "patient@example.com",
                LocalDate.of(2026, 5, 12),
                LocalTime.of(13, 0),
                LocalTime.of(13, 30),
                OffsetDateTime.now().minusMinutes(10),
                occurredAt
        ));

        verify(notificationSender).send(any(NotificationMessage.class));
        verify(notificationLogRepository).save(any());
    }

    @Test
    void reminderRequestedEventSkipsWhenAlreadyProcessed() {
        OffsetDateTime occurredAt = OffsetDateTime.now();
        when(notificationLogRepository.existsByAppointmentIdAndEventTypeAndRecipientEmailAndEventOccurredAt(
                16L,
                NotificationEventService.APPOINTMENT_REMINDER_REQUESTED,
                "patient@example.com",
                occurredAt
        )).thenReturn(true);

        notificationEventService.handleAppointmentReminderRequested(new AppointmentReminderRequestedEvent(
                16L,
                7L,
                "Dr. Gomez",
                100L,
                "Ana",
                "patient@example.com",
                LocalDate.of(2026, 5, 12),
                LocalTime.of(13, 0),
                LocalTime.of(13, 30),
                OffsetDateTime.now().minusMinutes(10),
                occurredAt
        ));

        verify(notificationSender, never()).send(any(NotificationMessage.class));
        verify(notificationLogRepository, never()).save(any());
    }
}
