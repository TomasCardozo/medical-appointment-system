package com.tomas.medical.doctor.controller;

import com.tomas.medical.doctor.config.SecurityConfig;
import com.tomas.medical.doctor.config.SecurityErrorHandler;
import com.tomas.medical.doctor.dto.response.InternalDoctorOwnerResponse;
import com.tomas.medical.doctor.dto.response.InternalDoctorSummaryResponse;
import com.tomas.medical.doctor.security.jwt.JwtAuthenticationFilter;
import com.tomas.medical.doctor.security.jwt.JwtService;
import com.tomas.medical.doctor.service.DoctorProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InternalDoctorController.class)
@AutoConfigureMockMvc(addFilters = true)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, SecurityErrorHandler.class})
class InternalDoctorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DoctorProfileService doctorProfileService;

    @MockBean
    private JwtService jwtService;

    @Test
    void internalDoctorByOwnerEmailIsAccessibleWithoutJwt() throws Exception {
        when(doctorProfileService.getDoctorByOwnerEmail("doctor@example.com"))
                .thenReturn(new InternalDoctorOwnerResponse(9L, "doctor@example.com", true));

        mockMvc.perform(get("/internal/doctors/by-owner-email")
                        .queryParam("email", "doctor@example.com")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(9))
                .andExpect(jsonPath("$.ownerEmail").value("doctor@example.com"));
    }

    @Test
    void internalDoctorByIdIsAccessibleWithoutJwt() throws Exception {
        when(doctorProfileService.getDoctorSummaryById(9L))
                .thenReturn(new InternalDoctorSummaryResponse(9L, "Dra. Ana Torres", true));

        mockMvc.perform(get("/internal/doctors/9")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(9))
                .andExpect(jsonPath("$.fullName").value("Dra. Ana Torres"))
                .andExpect(jsonPath("$.active").value(true));
    }
}
