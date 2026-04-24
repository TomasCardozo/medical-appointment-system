package com.tomas.medical.notification.service;

import com.tomas.medical.notification.dto.response.NotificationResponse;
import com.tomas.medical.notification.entity.NotificationLog;
import com.tomas.medical.notification.repository.NotificationLogRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class NotificationQueryService {

    private final NotificationLogRepository notificationLogRepository;

    public NotificationQueryService(NotificationLogRepository notificationLogRepository) {
        this.notificationLogRepository = notificationLogRepository;
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotifications(Authentication authentication,
                                                       Long appointmentId,
                                                       String eventType,
                                                       String status) {
        String scopedEmail = resolveScopedRecipientEmail(authentication);

        Specification<NotificationLog> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (scopedEmail != null) {
                predicates.add(cb.equal(cb.lower(root.get("recipientEmail")), scopedEmail.toLowerCase(Locale.ROOT)));
            }
            if (appointmentId != null) {
                predicates.add(cb.equal(root.get("appointmentId"), appointmentId));
            }
            if (eventType != null && !eventType.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("eventType")), eventType.toLowerCase(Locale.ROOT)));
            }
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("status")), status.toLowerCase(Locale.ROOT)));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };

        Sort sort = Sort.by(Sort.Order.desc("processedAt"), Sort.Order.desc("id"));

        return notificationLogRepository.findAll(specification, sort)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private String resolveScopedRecipientEmail(Authentication authentication) {
        if (hasRole(authentication, "ADMIN")) {
            return null;
        }
        return authentication.getName();
    }

    private boolean hasRole(Authentication authentication, String role) {
        String expected = "ROLE_" + role;
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(expected::equals);
    }

    private NotificationResponse toResponse(NotificationLog log) {
        return new NotificationResponse(
                log.getId(),
                log.getAppointmentId(),
                log.getEventType(),
                log.getRecipientEmail(),
                log.getProvider(),
                log.getStatus(),
                log.getErrorMessage(),
                log.getProcessedAt(),
                log.getEventOccurredAt(),
                log.getCreatedAt()
        );
    }
}
