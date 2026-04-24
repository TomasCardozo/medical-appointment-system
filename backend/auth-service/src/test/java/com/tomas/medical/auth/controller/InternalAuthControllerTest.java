package com.tomas.medical.auth.controller;

import com.tomas.medical.auth.config.SecurityConfig;
import com.tomas.medical.auth.config.SecurityErrorHandler;
import com.tomas.medical.auth.dto.response.InternalUserResponse;
import com.tomas.medical.auth.security.jwt.JwtAuthenticationFilter;
import com.tomas.medical.auth.security.jwt.JwtService;
import com.tomas.medical.auth.security.user.CustomUserDetailsService;
import com.tomas.medical.auth.service.AuthService;
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

@WebMvcTest(InternalAuthController.class)
@AutoConfigureMockMvc(addFilters = true)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, SecurityErrorHandler.class})
class InternalAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void internalUserByEmailIsAccessibleWithoutJwt() throws Exception {
        when(authService.getInternalUserByEmail("patient@example.com"))
                .thenReturn(new InternalUserResponse(1L, "Alice Patient", "patient@example.com", "PATIENT", true));

        mockMvc.perform(get("/internal/users/by-email")
                        .queryParam("email", "patient@example.com")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.fullName").value("Alice Patient"))
                .andExpect(jsonPath("$.email").value("patient@example.com"));
    }

    @Test
    void internalUserByIdIsAccessibleWithoutJwt() throws Exception {
        when(authService.getInternalUserById(2L))
                .thenReturn(new InternalUserResponse(2L, "Dr Bob", "doctor@example.com", "DOCTOR", true));

        mockMvc.perform(get("/internal/users/2")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.fullName").value("Dr Bob"))
                .andExpect(jsonPath("$.email").value("doctor@example.com"));
    }
}
