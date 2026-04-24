package com.tomas.medical.notification.controller;

import com.tomas.medical.notification.dto.response.NotificationResponse;
import com.tomas.medical.notification.service.NotificationQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationQueryService notificationQueryService;

    public NotificationController(NotificationQueryService notificationQueryService) {
        this.notificationQueryService = notificationQueryService;
    }

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getNotifications(Authentication authentication,
                                                                       @RequestParam(required = false) Long appointmentId,
                                                                       @RequestParam(required = false) String eventType,
                                                                       @RequestParam(required = false) String status) {
        return ResponseEntity.ok(notificationQueryService.getNotifications(authentication, appointmentId, eventType, status));
    }
}
