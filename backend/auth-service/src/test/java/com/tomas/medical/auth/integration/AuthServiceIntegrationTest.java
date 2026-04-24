package com.tomas.medical.auth.integration;

import com.tomas.medical.auth.dto.request.RegisterRequest;
import com.tomas.medical.auth.dto.request.UpdateUserProfileRequest;
import com.tomas.medical.auth.dto.response.UserMeResponse;
import com.tomas.medical.auth.entity.User;
import com.tomas.medical.auth.repository.UserRepository;
import com.tomas.medical.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "app.security.jwt.secret=replace-this-with-a-long-secret-key-at-least-32-bytes"
})
@Testcontainers(disabledWithoutDocker = true)
class AuthServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("auth_test_db")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @Transactional
    void registerPatientPersistsUserWithEncodedPasswordAndRole() {
        RegisterRequest request = new RegisterRequest(
                "Alice Patient",
                "alice.integration@example.com",
                "Password123!"
        );

        UserMeResponse response = authService.registerPatient(request);

        assertThat(response).isNotNull();
        assertThat(response.role()).isEqualTo("PATIENT");

        User persisted = userRepository.findByEmail("alice.integration@example.com").orElseThrow();
        assertThat(persisted.getRole().getName().name()).isEqualTo("PATIENT");
        assertThat(persisted.getPasswordHash()).isNotEqualTo("Password123!");
        assertThat(persisted.isActive()).isTrue();
    }

    @Test
    @Transactional
    void updateMeUpdatesFullNameForAuthenticatedUser() {
        RegisterRequest registerRequest = new RegisterRequest(
                "Alice Original",
                "alice.update@example.com",
                "Password123!"
        );

        authService.registerPatient(registerRequest);

        UserMeResponse response = authService.updateMe(
                "alice.update@example.com",
                new UpdateUserProfileRequest("Alice Updated", null, null)
        );

        assertThat(response.fullName()).isEqualTo("Alice Updated");
        User persisted = userRepository.findByEmail("alice.update@example.com").orElseThrow();
        assertThat(persisted.getFullName()).isEqualTo("Alice Updated");
    }
}
