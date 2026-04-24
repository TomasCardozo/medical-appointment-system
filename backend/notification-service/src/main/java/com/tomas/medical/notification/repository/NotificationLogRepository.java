package com.tomas.medical.notification.repository;

import com.tomas.medical.notification.entity.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.OffsetDateTime;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long>,
        JpaSpecificationExecutor<NotificationLog> {

    boolean existsByAppointmentIdAndEventTypeAndRecipientEmailAndEventOccurredAt(Long appointmentId,
                                                                                  String eventType,
                                                                                  String recipientEmail,
                                                                                  OffsetDateTime eventOccurredAt);
}
