package com.tomas.medical.notification.integration;

import com.tomas.medical.notification.entity.NotificationLog;
import com.tomas.medical.notification.repository.NotificationLogRepository;
import com.tomas.medical.notification.security.jwt.JwtService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "app.security.jwt.secret=replace-this-with-a-long-secret-key-at-least-32-bytes",
        "app.kafka.topics.appointment-created=appointment.created",
        "app.kafka.topics.appointment-cancelled=appointment.cancelled",
        "app.kafka.topics.appointment-rescheduled=appointment.rescheduled",
        "app.kafka.topics.appointment-reminder-requested=appointment.reminder.requested",
        "spring.kafka.consumer.group-id=notification-service-test",
        "spring.kafka.listener.auto-startup=false"
})
@AutoConfigureMockMvc(addFilters = true)
@Testcontainers(disabledWithoutDocker = true)
class NotificationHistoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("notification_test_db")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NotificationLogRepository notificationLogRepository;

    @MockBean
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        notificationLogRepository.deleteAll();
    }

    @Test
    void patientOnlySeesOwnNotifications() throws Exception {
        mockPatientToken("patient.one@example.com");

        notificationLogRepository.save(notification(
                101L,
                "appointment.created",
                "patient.one@example.com",
                "SENT",
                OffsetDateTime.now().minusHours(1)
        ));
        notificationLogRepository.save(notification(
                202L,
                "appointment.cancelled",
                "patient.two@example.com",
                "SENT",
                OffsetDateTime.now().minusMinutes(30)
        ));

        mockMvc.perform(get("/notifications")
                        .header("Authorization", "Bearer patient-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].recipientEmail").value("patient.one@example.com"));
    }

    @Test
    void adminCanFilterByEventTypeAndStatus() throws Exception {
        mockAdminToken();

        notificationLogRepository.save(notification(
                303L,
                "appointment.rescheduled",
                "patient.one@example.com",
                "SENT",
                OffsetDateTime.now().minusHours(2)
        ));
        notificationLogRepository.save(notification(
                404L,
                "appointment.rescheduled",
                "patient.two@example.com",
                "FAILED",
                OffsetDateTime.now().minusHours(1)
        ));
        notificationLogRepository.save(notification(
                505L,
                "appointment.cancelled",
                "patient.one@example.com",
                "SENT",
                OffsetDateTime.now().minusMinutes(10)
        ));

        mockMvc.perform(get("/notifications")
                        .queryParam("eventType", "appointment.rescheduled")
                        .queryParam("status", "SENT")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].eventType").value("appointment.rescheduled"))
                .andExpect(jsonPath("$[0].status").value("SENT"));
    }

    private void mockPatientToken(String email) {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn(email);
        when(claims.get("role", String.class)).thenReturn("PATIENT");
        when(jwtService.extractAllClaims("patient-token")).thenReturn(claims);
    }

    private void mockAdminToken() {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("admin@example.com");
        when(claims.get("role", String.class)).thenReturn("ADMIN");
        when(jwtService.extractAllClaims("admin-token")).thenReturn(claims);
    }

    private NotificationLog notification(Long appointmentId,
                                         String eventType,
                                         String recipientEmail,
                                         String status,
                                         OffsetDateTime processedAt) {
        NotificationLog log = new NotificationLog();
        log.setAppointmentId(appointmentId);
        log.setEventType(eventType);
        log.setRecipientEmail(recipientEmail);
        log.setProvider("log");
        log.setStatus(status);
        log.setProcessedAt(processedAt);
        log.setEventOccurredAt(processedAt.minusMinutes(1));
        return log;
    }
}
