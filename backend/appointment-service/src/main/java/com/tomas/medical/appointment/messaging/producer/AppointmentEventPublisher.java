package com.tomas.medical.appointment.messaging.producer;

import com.tomas.medical.appointment.dto.event.AppointmentCancelledEvent;
import com.tomas.medical.appointment.dto.event.AppointmentCreatedEvent;
import com.tomas.medical.appointment.dto.event.AppointmentReminderRequestedEvent;
import com.tomas.medical.appointment.dto.event.AppointmentRescheduledEvent;
import com.tomas.medical.appointment.messaging.config.AppointmentTopicsProperties;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class AppointmentEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final AppointmentTopicsProperties topics;

    public AppointmentEventPublisher(KafkaTemplate<String, Object> kafkaTemplate,
                                     AppointmentTopicsProperties topics) {
        this.kafkaTemplate = kafkaTemplate;
        this.topics = topics;
    }

    public void publishCreated(AppointmentCreatedEvent event) {
        kafkaTemplate.send(topics.appointmentCreated(), String.valueOf(event.appointmentId()), event);
    }

    public void publishCancelled(AppointmentCancelledEvent event) {
        kafkaTemplate.send(topics.appointmentCancelled(), String.valueOf(event.appointmentId()), event);
    }

    public void publishRescheduled(AppointmentRescheduledEvent event) {
        kafkaTemplate.send(topics.appointmentRescheduled(), String.valueOf(event.appointmentId()), event);
    }

    public void publishReminderRequested(AppointmentReminderRequestedEvent event) {
        kafkaTemplate.send(topics.appointmentReminderRequested(), String.valueOf(event.appointmentId()), event);
    }
}
