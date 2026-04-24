package com.tomas.medical.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.notifications.provider", havingValue = "log", matchIfMissing = true)
public class LoggingNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingNotificationSender.class);

    @Override
    public String providerName() {
        return "log";
    }

    @Override
    public void send(NotificationMessage message) {
        log.info("Notification stub -> to={} subject={} body={}", message.to(), message.subject(), message.body());
    }
}
