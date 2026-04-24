package com.tomas.medical.notification.dto.response;

import java.time.OffsetDateTime;

public record NotificationResponse(
        Long id,
        Long appointmentId,
        String eventType,
        String recipientEmail,
        String provider,
        String status,
        String errorMessage,
        OffsetDateTime processedAt,
        OffsetDateTime eventOccurredAt,
        OffsetDateTime createdAt
) {
}
