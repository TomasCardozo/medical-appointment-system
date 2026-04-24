package com.tomas.medical.notification.messaging.consumer;

import com.tomas.medical.notification.dto.event.AppointmentReminderRequestedEvent;
import com.tomas.medical.notification.service.NotificationEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AppointmentEventsConsumerTest {

    @Mock
    private NotificationEventService notificationEventService;

    private AppointmentEventsConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new AppointmentEventsConsumer(notificationEventService);
    }

    @Test
    void onAppointmentReminderRequestedDelegatesToService() {
        AppointmentReminderRequestedEvent event = new AppointmentReminderRequestedEvent(
                22L,
                7L,
                "Dr. Gomez",
                100L,
                "Ana",
                "patient@example.com",
                LocalDate.of(2026, 5, 20),
                LocalTime.of(10, 0),
                LocalTime.of(10, 30),
                OffsetDateTime.now().minusMinutes(5),
                OffsetDateTime.now()
        );

        consumer.onAppointmentReminderRequested(event);

        verify(notificationEventService).handleAppointmentReminderRequested(event);
    }
}
