package com.tomas.medical.notification.controller;

import com.tomas.medical.notification.config.SecurityConfig;
import com.tomas.medical.notification.config.SecurityErrorHandler;
import com.tomas.medical.notification.dto.response.NotificationResponse;
import com.tomas.medical.notification.security.jwt.JwtAuthenticationFilter;
import com.tomas.medical.notification.security.jwt.JwtService;
import com.tomas.medical.notification.service.NotificationQueryService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
@AutoConfigureMockMvc(addFilters = true)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, SecurityErrorHandler.class})
class NotificationControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationQueryService notificationQueryService;

    @MockBean
    private JwtService jwtService;

    @Test
    void getNotificationsReturnsUnauthorizedWhenTokenMissing() throws Exception {
        mockMvc.perform(get("/notifications").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getNotificationsReturnsForbiddenForDoctorRole() throws Exception {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("doctor@example.com");
        when(claims.get("role", String.class)).thenReturn("DOCTOR");
        when(jwtService.extractAllClaims("doctor-token")).thenReturn(claims);

        mockMvc.perform(get("/notifications")
                        .header("Authorization", "Bearer doctor-token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void getNotificationsAllowsPatientRole() throws Exception {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("patient@example.com");
        when(claims.get("role", String.class)).thenReturn("PATIENT");
        when(jwtService.extractAllClaims("patient-token")).thenReturn(claims);
        when(notificationQueryService.getNotifications(any(), any(), any(), any()))
                .thenReturn(List.of(new NotificationResponse(
                        1L,
                        10L,
                        "appointment.created",
                        "patient@example.com",
                        "log",
                        "SENT",
                        null,
                        OffsetDateTime.now(),
                        OffsetDateTime.now(),
                        OffsetDateTime.now()
                )));

        mockMvc.perform(get("/notifications")
                        .header("Authorization", "Bearer patient-token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
