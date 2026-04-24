package com.tomas.medical.appointment.messaging.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.kafka.topics")
public record AppointmentTopicsProperties(
        String appointmentCreated,
        String appointmentCancelled,
        String appointmentRescheduled,
        String appointmentReminderRequested
) {
}
