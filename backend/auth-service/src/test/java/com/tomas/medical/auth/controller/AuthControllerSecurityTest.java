package com.tomas.medical.auth.controller;

import com.tomas.medical.auth.config.SecurityConfig;
import com.tomas.medical.auth.config.SecurityErrorHandler;
import com.tomas.medical.auth.dto.response.UserMeResponse;
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
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = true)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, SecurityErrorHandler.class})
class AuthControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void meReturnsUnauthorizedWhenTokenIsMissing() throws Exception {
        mockMvc.perform(get("/auth/me").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meReturnsUserWhenTokenIsValid() throws Exception {
        UserDetails userDetails = User
                .withUsername("alice@example.com")
                .password("ignored")
                .authorities("ROLE_PATIENT")
                .build();

        when(jwtService.extractUsername("valid-token")).thenReturn("alice@example.com");
        when(jwtService.isTokenValid("valid-token", "alice@example.com")).thenReturn(true);
        when(customUserDetailsService.loadUserByUsername("alice@example.com")).thenReturn(userDetails);
        when(authService.me("alice@example.com"))
                .thenReturn(new UserMeResponse(1L, "Alice", "alice@example.com", "PATIENT"));

        mockMvc.perform(get("/auth/me")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.role").value("PATIENT"));
    }

    @Test
    void updateMeReturnsUnauthorizedWhenTokenIsMissing() throws Exception {
        String requestBody = """
                {
                  "fullName": "Alice Updated"
                }
                """;

        mockMvc.perform(put("/auth/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateMeReturnsUserWhenTokenIsValid() throws Exception {
        UserDetails userDetails = User
                .withUsername("alice@example.com")
                .password("ignored")
                .authorities("ROLE_PATIENT")
                .build();

        when(jwtService.extractUsername("valid-token")).thenReturn("alice@example.com");
        when(jwtService.isTokenValid("valid-token", "alice@example.com")).thenReturn(true);
        when(customUserDetailsService.loadUserByUsername("alice@example.com")).thenReturn(userDetails);
        when(authService.updateMe(org.mockito.ArgumentMatchers.eq("alice@example.com"), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new UserMeResponse(1L, "Alice Updated", "alice@example.com", "PATIENT"));

        String requestBody = """
                {
                  "fullName": "Alice Updated"
                }
                """;

        mockMvc.perform(put("/auth/me")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.fullName").value("Alice Updated"));
    }
}
