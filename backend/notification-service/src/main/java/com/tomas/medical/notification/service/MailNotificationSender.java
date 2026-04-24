package com.tomas.medical.notification.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.notifications.provider", havingValue = "mail")
public class MailNotificationSender implements NotificationSender {

    private final JavaMailSender javaMailSender;
    private final String fromAddress;

    public MailNotificationSender(JavaMailSender javaMailSender,
                                  NotificationSenderProperties properties) {
        this.javaMailSender = javaMailSender;
        this.fromAddress = properties.fromAddress();
    }

    @Override
    public String providerName() {
        return "mail";
    }

    @Override
    public void send(NotificationMessage message) {
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setFrom(fromAddress);
        mailMessage.setTo(message.to());
        mailMessage.setSubject(message.subject());
        mailMessage.setText(message.body());
        javaMailSender.send(mailMessage);
    }
}
