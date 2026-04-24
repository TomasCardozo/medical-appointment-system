package com.tomas.medical.notification.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.notifications")
public record NotificationSenderProperties(
        String provider,
        String fromAddress
) {
}
