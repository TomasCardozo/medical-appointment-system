package com.tomas.medical.notification.service;

public interface NotificationSender {

    String providerName();

    void send(NotificationMessage message);
}
