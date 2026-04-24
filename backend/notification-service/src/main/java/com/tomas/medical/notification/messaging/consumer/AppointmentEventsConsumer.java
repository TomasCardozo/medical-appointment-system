package com.tomas.medical.notification.messaging.consumer;

import com.tomas.medical.notification.dto.event.AppointmentCancelledEvent;
import com.tomas.medical.notification.dto.event.AppointmentCreatedEvent;
import com.tomas.medical.notification.dto.event.AppointmentReminderRequestedEvent;
import com.tomas.medical.notification.dto.event.AppointmentRescheduledEvent;
import com.tomas.medical.notification.service.NotificationEventService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class AppointmentEventsConsumer {

    private final NotificationEventService notificationEventService;

    public AppointmentEventsConsumer(NotificationEventService notificationEventService) {
        this.notificationEventService = notificationEventService;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.appointment-created}",
            containerFactory = "appointmentCreatedKafkaListenerContainerFactory"
    )
    public void onAppointmentCreated(AppointmentCreatedEvent event) {
        notificationEventService.handleAppointmentCreated(event);
    }

    @KafkaListener(
            topics = "${app.kafka.topics.appointment-cancelled}",
            containerFactory = "appointmentCancelledKafkaListenerContainerFactory"
    )
    public void onAppointmentCancelled(AppointmentCancelledEvent event) {
        notificationEventService.handleAppointmentCancelled(event);
    }

    @KafkaListener(
            topics = "${app.kafka.topics.appointment-rescheduled}",
            containerFactory = "appointmentRescheduledKafkaListenerContainerFactory"
    )
    public void onAppointmentRescheduled(AppointmentRescheduledEvent event) {
        notificationEventService.handleAppointmentRescheduled(event);
    }

    @KafkaListener(
            topics = "${app.kafka.topics.appointment-reminder-requested}",
            containerFactory = "appointmentReminderRequestedKafkaListenerContainerFactory"
    )
    public void onAppointmentReminderRequested(AppointmentReminderRequestedEvent event) {
        notificationEventService.handleAppointmentReminderRequested(event);
    }
}
