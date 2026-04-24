package com.tomas.medical.notification;

import com.tomas.medical.notification.config.NotificationTopicsProperties;
import com.tomas.medical.notification.service.NotificationSenderProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({NotificationTopicsProperties.class, NotificationSenderProperties.class})
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
