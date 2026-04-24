package com.tomas.medical.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.kafka.topics")
public record NotificationTopicsProperties(
        String appointmentCreated,
        String appointmentCancelled,
        String appointmentRescheduled,
        String appointmentReminderRequested
) {
}
