package com.tomas.medical.appointment.integration;

import com.tomas.medical.appointment.client.AuthServiceClient;
import com.tomas.medical.appointment.client.DoctorServiceClient;
import com.tomas.medical.appointment.client.dto.DoctorScheduleResponse;
import com.tomas.medical.appointment.client.dto.InternalUserResponse;
import com.tomas.medical.appointment.dto.request.CreateAppointmentRequest;
import com.tomas.medical.appointment.dto.request.RescheduleAppointmentRequest;
import com.tomas.medical.appointment.dto.response.AppointmentResponse;
import com.tomas.medical.appointment.entity.Appointment;
import com.tomas.medical.appointment.enumtype.AppointmentStatus;
import com.tomas.medical.appointment.messaging.producer.AppointmentEventPublisher;
import com.tomas.medical.appointment.repository.AppointmentRepository;
import com.tomas.medical.appointment.service.AppointmentReminderService;
import com.tomas.medical.appointment.service.AppointmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "app.security.jwt.secret=replace-this-with-a-long-secret-key-at-least-32-bytes"
})
@Testcontainers(disabledWithoutDocker = true)
class AppointmentServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("appointment_test_db")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private AppointmentReminderService appointmentReminderService;

    @MockBean
    private DoctorServiceClient doctorServiceClient;

    @MockBean
    private AuthServiceClient authServiceClient;

    @MockBean
    private AppointmentEventPublisher appointmentEventPublisher;

    @BeforeEach
    void setUp() {
        appointmentRepository.deleteAll();
    }

    @Test
    void createAppointmentPersistsBookedAppointmentWhenSlotIsAvailable() {
        LocalDate date = LocalDate.now().plusDays(2);

        when(authServiceClient.getUserByEmail("patient.integration@example.com"))
                .thenReturn(new InternalUserResponse(100L, "Patient Integration", "patient.integration@example.com", "PATIENT", true));
        when(doctorServiceClient.getDoctorSchedule(7L, date, date))
                .thenReturn(schedule(date));

        AppointmentResponse response = appointmentService.createAppointment(
                new CreateAppointmentRequest(7L, date, LocalTime.of(9, 0), null),
                new UsernamePasswordAuthenticationToken(
                        "patient.integration@example.com",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_PATIENT"))
                )
        );

        assertThat(response.id()).isNotNull();
        assertThat(response.status()).isEqualTo("BOOKED");
        assertThat(response.patientId()).isEqualTo(100L);
        assertThat(appointmentRepository.count()).isEqualTo(1L);
    }

    @Test
    void rescheduleAppointmentUpdatesSlotAndPersistsPreviousSlotMetadata() {
        LocalDate initialDate = LocalDate.now().plusDays(3);
        LocalDate newDate = initialDate.plusDays(1);

        when(authServiceClient.getUserByEmail("patient.integration@example.com"))
                .thenReturn(new InternalUserResponse(100L, "Patient Integration", "patient.integration@example.com", "PATIENT", true));
        when(authServiceClient.getUserById(100L))
                .thenReturn(new InternalUserResponse(100L, "Patient Integration", "patient.integration@example.com", "PATIENT", true));
        when(doctorServiceClient.getDoctorById(7L))
                .thenReturn(new com.tomas.medical.appointment.client.dto.InternalDoctorSummaryResponse(7L, "Dr. Gomez", true));
        when(doctorServiceClient.getDoctorSchedule(7L, initialDate, initialDate)).thenReturn(schedule(initialDate));
        when(doctorServiceClient.getDoctorSchedule(7L, newDate, newDate)).thenReturn(schedule(newDate));

        AppointmentResponse created = appointmentService.createAppointment(
                new CreateAppointmentRequest(7L, initialDate, LocalTime.of(9, 0), null),
                new UsernamePasswordAuthenticationToken(
                        "patient.integration@example.com",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_PATIENT"))
                )
        );

        Appointment beforeReschedule = appointmentRepository.findById(created.id()).orElseThrow();
        beforeReschedule.setReminderRequestedAt(OffsetDateTime.now().minusHours(1));
        appointmentRepository.save(beforeReschedule);

        AppointmentResponse rescheduled = appointmentService.rescheduleAppointment(
                created.id(),
                new RescheduleAppointmentRequest(newDate, LocalTime.of(9, 30), "Cambio de agenda"),
                new UsernamePasswordAuthenticationToken(
                        "patient.integration@example.com",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_PATIENT"))
                )
        );

        Appointment persisted = appointmentRepository.findById(created.id()).orElseThrow();

        assertThat(rescheduled.id()).isEqualTo(created.id());
        assertThat(rescheduled.appointmentDate()).isEqualTo(newDate);
        assertThat(rescheduled.startTime()).isEqualTo(LocalTime.of(9, 30));
        assertThat(persisted.getPreviousAppointmentDate()).isEqualTo(initialDate);
        assertThat(persisted.getPreviousStartTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(persisted.getPreviousEndTime()).isEqualTo(LocalTime.of(9, 30));
        assertThat(persisted.getRescheduledAt()).isNotNull();
        assertThat(persisted.getReminderRequestedAt()).isNull();
    }

    @Test
    void reminderScanMarksAppointmentToAvoidDuplicateReminder() {
        LocalDateTime start = LocalDateTime.now().plusHours(2).withSecond(0).withNano(0);

        Appointment appointment = new Appointment();
        appointment.setDoctorId(7L);
        appointment.setPatientId(100L);
        appointment.setAppointmentDate(start.toLocalDate());
        appointment.setStartTime(start.toLocalTime());
        appointment.setEndTime(start.plusMinutes(30).toLocalTime());
        appointment.setStatus(AppointmentStatus.BOOKED);
        Appointment persisted = appointmentRepository.save(appointment);

        when(authServiceClient.getUserById(100L))
                .thenReturn(new InternalUserResponse(100L, "Patient Integration", "patient.integration@example.com", "PATIENT", true));
        when(doctorServiceClient.getDoctorById(7L))
                .thenReturn(new com.tomas.medical.appointment.client.dto.InternalDoctorSummaryResponse(7L, "Dr. Gomez", true));

        appointmentReminderService.scanUpcomingAppointmentsForReminders();

        Appointment updated = appointmentRepository.findById(persisted.getId()).orElseThrow();
        assertThat(updated.getReminderRequestedAt()).isNotNull();
        verify(appointmentEventPublisher).publishReminderRequested(any());
    }

    private DoctorScheduleResponse schedule(LocalDate date) {
        DoctorScheduleResponse.DoctorScheduleWindowResponse window =
                new DoctorScheduleResponse.DoctorScheduleWindowResponse(
                        1L,
                        LocalTime.of(9, 0),
                        LocalTime.of(10, 0),
                        30
                );

        DoctorScheduleResponse.DoctorScheduleDayResponse day =
                new DoctorScheduleResponse.DoctorScheduleDayResponse(
                        date,
                        DayOfWeek.MONDAY,
                        List.of(window),
                        List.of()
                );

        return new DoctorScheduleResponse(
                7L,
                date,
                date,
                List.of(),
                List.of(day)
        );
    }
}
