package com.tomas.medical.doctor.controller;

import com.tomas.medical.doctor.config.SecurityConfig;
import com.tomas.medical.doctor.config.SecurityErrorHandler;
import com.tomas.medical.doctor.dto.response.DoctorProfileResponse;
import com.tomas.medical.doctor.security.jwt.JwtAuthenticationFilter;
import com.tomas.medical.doctor.security.jwt.JwtService;
import com.tomas.medical.doctor.service.DoctorAvailabilityService;
import com.tomas.medical.doctor.service.DoctorProfileService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DoctorController.class)
@AutoConfigureMockMvc(addFilters = true)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, SecurityErrorHandler.class})
class DoctorControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DoctorProfileService doctorProfileService;

    @MockBean
    private DoctorAvailabilityService doctorAvailabilityService;

    @MockBean
    private JwtService jwtService;

    @Test
    void listDoctorsIsPublic() throws Exception {
        when(doctorProfileService.listProfiles())
                .thenReturn(List.of(new DoctorProfileResponse(1L, "Dra. Ana", "Cardiology", "MAT-11", null, null)));

        mockMvc.perform(get("/doctors").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void getMyDoctorReturnsUnauthorizedWhenTokenMissing() throws Exception {
        mockMvc.perform(get("/doctors/me").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMyDoctorAllowsAnyAuthenticatedRole() throws Exception {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("patient@example.com");
        when(claims.get("role", String.class)).thenReturn("PATIENT");
        when(jwtService.extractAllClaims("patient-token")).thenReturn(claims);

        when(doctorProfileService.getMyProfile(any()))
                .thenReturn(new DoctorProfileResponse(1L, "Dra. Ana", "Cardiology", "MAT-11", null, null));

        mockMvc.perform(get("/doctors/me")
                        .header("Authorization", "Bearer patient-token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void createProfileReturnsUnauthorizedWhenTokenMissing() throws Exception {
        String requestBody = """
                {
                  "fullName": "Dra. Ana",
                  "specialty": "Cardiology",
                  "licenseNumber": "MAT-11"
                }
                """;

        mockMvc.perform(post("/doctors/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createAvailabilityReturnsForbiddenForPatientRole() throws Exception {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("patient@example.com");
        when(claims.get("role", String.class)).thenReturn("PATIENT");
        when(jwtService.extractAllClaims("patient-token")).thenReturn(claims);

        String requestBody = """
                {
                  "dayOfWeek": "MONDAY",
                  "startTime": "09:00",
                  "endTime": "12:00",
                  "slotDurationMinutes": 30
                }
                """;

        mockMvc.perform(post("/doctors/1/availability")
                        .header("Authorization", "Bearer patient-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden());
    }

    @Test
    void createProfileReturnsForbiddenForAdminRole() throws Exception {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("admin@example.com");
        when(claims.get("role", String.class)).thenReturn("ADMIN");
        when(jwtService.extractAllClaims("admin-token")).thenReturn(claims);

        String requestBody = """
                {
                  "fullName": "Dr. Admin",
                  "specialty": "Cardiology",
                  "licenseNumber": "MAT-99"
                }
                """;

        mockMvc.perform(post("/doctors/profile")
                        .header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateMyDoctorReturnsUnauthorizedWhenTokenMissing() throws Exception {
        String requestBody = """
                {
                  "fullName": "Dra. Updated",
                  "specialty": "Cardiology",
                  "licenseNumber": "MAT-11"
                }
                """;

        mockMvc.perform(put("/doctors/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateMyDoctorReturnsForbiddenForPatientRole() throws Exception {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("patient@example.com");
        when(claims.get("role", String.class)).thenReturn("PATIENT");
        when(jwtService.extractAllClaims("patient-token")).thenReturn(claims);

        String requestBody = """
                {
                  "fullName": "Dra. Updated",
                  "specialty": "Cardiology",
                  "licenseNumber": "MAT-11"
                }
                """;

        mockMvc.perform(put("/doctors/me")
                        .header("Authorization", "Bearer patient-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateMyDoctorAllowsDoctorRole() throws Exception {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("doctor@example.com");
        when(claims.get("role", String.class)).thenReturn("DOCTOR");
        when(jwtService.extractAllClaims("doctor-token")).thenReturn(claims);

        when(doctorProfileService.updateMyProfile(any(), any()))
                .thenReturn(new DoctorProfileResponse(1L, "Dra. Updated", "Cardiology", "MAT-11", null, null));

        String requestBody = """
                {
                  "fullName": "Dra. Updated",
                  "specialty": "Cardiology",
                  "licenseNumber": "MAT-11"
                }
                """;

        mockMvc.perform(put("/doctors/me")
                        .header("Authorization", "Bearer doctor-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.fullName").value("Dra. Updated"));
    }
}
