package com.tomas.medical.doctor.integration;

import com.tomas.medical.doctor.dto.request.CreateDoctorAvailabilityRequest;
import com.tomas.medical.doctor.dto.request.CreateDoctorProfileRequest;
import com.tomas.medical.doctor.dto.response.DoctorProfileResponse;
import com.tomas.medical.doctor.dto.response.DoctorScheduleResponse;
import com.tomas.medical.doctor.repository.BlockedSlotRepository;
import com.tomas.medical.doctor.repository.DoctorAvailabilityRepository;
import com.tomas.medical.doctor.repository.DoctorProfileRepository;
import com.tomas.medical.doctor.service.DoctorAvailabilityService;
import com.tomas.medical.doctor.service.DoctorProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "app.security.jwt.secret=replace-this-with-a-long-secret-key-at-least-32-bytes"
})
@Testcontainers(disabledWithoutDocker = true)
class DoctorAvailabilityIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("doctor_test_db")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private DoctorProfileService doctorProfileService;

    @Autowired
    private DoctorAvailabilityService doctorAvailabilityService;

    @Autowired
    private BlockedSlotRepository blockedSlotRepository;

    @Autowired
    private DoctorAvailabilityRepository doctorAvailabilityRepository;

    @Autowired
    private DoctorProfileRepository doctorProfileRepository;

    @BeforeEach
    void setUp() {
        blockedSlotRepository.deleteAll();
        doctorAvailabilityRepository.deleteAll();
        doctorProfileRepository.deleteAll();
    }

    @Test
    void createProfileAndAvailabilityThenReturnsScheduleForDateRange() {
        UsernamePasswordAuthenticationToken doctorAuth = new UsernamePasswordAuthenticationToken(
                "doctor.integration@example.com",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_DOCTOR"))
        );

        DoctorProfileResponse profile = doctorProfileService.createProfile(
                new CreateDoctorProfileRequest(
                        "Dr Integration",
                        "Cardiology",
                        "LIC-INT-001",
                        "Main St 123",
                        "Integration test profile"
                ),
                doctorAuth
        );

        doctorAvailabilityService.createAvailability(
                profile.id(),
                new CreateDoctorAvailabilityRequest(
                        DayOfWeek.MONDAY,
                        LocalTime.of(9, 0),
                        LocalTime.of(11, 0),
                        30
                ),
                doctorAuth
        );

        LocalDate monday = LocalDate.of(2026, 4, 20);
        DoctorScheduleResponse schedule = doctorAvailabilityService.getSchedule(profile.id(), monday, monday);

        assertThat(schedule.doctorId()).isEqualTo(profile.id());
        assertThat(schedule.availabilityRules()).hasSize(1);
        assertThat(schedule.days()).hasSize(1);
        assertThat(schedule.days().getFirst().availableWindows()).hasSize(1);
        assertThat(schedule.days().getFirst().availableWindows().getFirst().startTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(schedule.days().getFirst().availableWindows().getFirst().endTime()).isEqualTo(LocalTime.of(11, 0));
    }
}
