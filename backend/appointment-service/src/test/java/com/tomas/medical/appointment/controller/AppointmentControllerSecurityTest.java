package com.tomas.medical.appointment.controller;

import com.tomas.medical.appointment.config.SecurityConfig;
import com.tomas.medical.appointment.config.SecurityErrorHandler;
import com.tomas.medical.appointment.dto.response.AppointmentResponse;
import com.tomas.medical.appointment.security.jwt.JwtAuthenticationFilter;
import com.tomas.medical.appointment.security.jwt.JwtService;
import com.tomas.medical.appointment.service.AppointmentService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AppointmentController.class)
@AutoConfigureMockMvc(addFilters = true)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, SecurityErrorHandler.class})
class AppointmentControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AppointmentService appointmentService;

    @MockBean
    private JwtService jwtService;

    @Test
    void createAppointmentReturnsUnauthorizedWhenTokenMissing() throws Exception {
        String requestBody = """
                {
                  "doctorId": 1,
                  "appointmentDate": "2026-05-20",
                  "startTime": "09:00"
                }
                """;

        mockMvc.perform(post("/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createAppointmentReturnsForbiddenForDoctorRole() throws Exception {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("doctor@example.com");
        when(claims.get("role", String.class)).thenReturn("DOCTOR");
        when(jwtService.extractAllClaims("doctor-token")).thenReturn(claims);

        String requestBody = """
                {
                  "doctorId": 1,
                  "appointmentDate": "2026-05-20",
                  "startTime": "09:00"
                }
                """;

        mockMvc.perform(post("/appointments")
                        .header("Authorization", "Bearer doctor-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden());
    }

    @Test
    void patientCanAccessOwnAppointmentsEndpoint() throws Exception {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("patient@example.com");
        when(claims.get("role", String.class)).thenReturn("PATIENT");
        when(jwtService.extractAllClaims("patient-token")).thenReturn(claims);
        when(appointmentService.getPatientAppointments(any(), any()))
                .thenReturn(List.of(new AppointmentResponse(
                        1L,
                        3L,
                        null,
                        10L,
                        null,
                        LocalDate.of(2026, 5, 20),
                        LocalTime.of(9, 0),
                        LocalTime.of(9, 30),
                        "BOOKED",
                        null,
                        null
                )));

        mockMvc.perform(get("/appointments/patient/10")
                        .header("Authorization", "Bearer patient-token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void availableSlotsReturnsBadRequestWhenDoctorIdIsMissing() throws Exception {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("patient@example.com");
        when(claims.get("role", String.class)).thenReturn("PATIENT");
        when(jwtService.extractAllClaims("patient-token")).thenReturn(claims);

        mockMvc.perform(get("/appointments/available")
                        .queryParam("date", "2026-05-20")
                        .header("Authorization", "Bearer patient-token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Missing required parameter: doctorId"));
    }

    @Test
    void rescheduleAppointmentReturnsUnauthorizedWhenTokenMissing() throws Exception {
        String requestBody = """
                {
                  "appointmentDate": "2026-05-21",
                  "startTime": "10:00"
                }
                """;

        mockMvc.perform(put("/appointments/10/reschedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rescheduleAppointmentReturnsForbiddenForDoctorRole() throws Exception {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("doctor@example.com");
        when(claims.get("role", String.class)).thenReturn("DOCTOR");
        when(jwtService.extractAllClaims("doctor-token")).thenReturn(claims);

        String requestBody = """
                {
                  "appointmentDate": "2026-05-21",
                  "startTime": "10:00"
                }
                """;

        mockMvc.perform(put("/appointments/10/reschedule")
                        .header("Authorization", "Bearer doctor-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden());
    }

    @Test
    void patientCanRescheduleAppointment() throws Exception {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("patient@example.com");
        when(claims.get("role", String.class)).thenReturn("PATIENT");
        when(jwtService.extractAllClaims("patient-token")).thenReturn(claims);
        when(appointmentService.rescheduleAppointment(any(), any(), any()))
                .thenReturn(new AppointmentResponse(
                        10L,
                        3L,
                        null,
                        10L,
                        null,
                        LocalDate.of(2026, 5, 21),
                        LocalTime.of(10, 0),
                        LocalTime.of(10, 30),
                        "BOOKED",
                        null,
                        null
                ));

        String requestBody = """
                {
                  "appointmentDate": "2026-05-21",
                  "startTime": "10:00"
                }
                """;

        mockMvc.perform(put("/appointments/10/reschedule")
                        .header("Authorization", "Bearer patient-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());
    }

    @Test
    void rescheduleAppointmentReturnsBadRequestWhenPayloadIsInvalid() throws Exception {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("patient@example.com");
        when(claims.get("role", String.class)).thenReturn("PATIENT");
        when(jwtService.extractAllClaims("patient-token")).thenReturn(claims);

        String requestBody = """
                {
                  "startTime": "10:00"
                }
                """;

        mockMvc.perform(put("/appointments/10/reschedule")
                        .header("Authorization", "Bearer patient-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("appointmentDate is required"));
    }
}
