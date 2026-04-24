package com.tomas.medical.notification.service;

public record NotificationMessage(
        String to,
        String subject,
        String body
) {
}
