package com.tomas.medical.appointment.service;

import com.tomas.medical.appointment.client.AuthServiceClient;
import com.tomas.medical.appointment.client.DoctorServiceClient;
import com.tomas.medical.appointment.client.dto.DoctorScheduleResponse;
import com.tomas.medical.appointment.client.dto.InternalDoctorSummaryResponse;
import com.tomas.medical.appointment.client.dto.InternalUserResponse;
import com.tomas.medical.appointment.dto.request.CancelAppointmentRequest;
import com.tomas.medical.appointment.dto.request.CreateAppointmentRequest;
import com.tomas.medical.appointment.dto.request.RescheduleAppointmentRequest;
import com.tomas.medical.appointment.dto.response.AppointmentResponse;
import com.tomas.medical.appointment.dto.response.AvailableSlotResponse;
import com.tomas.medical.appointment.entity.Appointment;
import com.tomas.medical.appointment.enumtype.AppointmentStatus;
import com.tomas.medical.appointment.exception.AppointmentConflictException;
import com.tomas.medical.appointment.exception.SlotUnavailableException;
import com.tomas.medical.appointment.mapper.AppointmentMapper;
import com.tomas.medical.appointment.messaging.producer.AppointmentEventPublisher;
import com.tomas.medical.appointment.repository.AppointmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.DayOfWeek;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private DoctorServiceClient doctorServiceClient;

    @Mock
    private AuthServiceClient authServiceClient;

    @Mock
    private AppointmentEventPublisher appointmentEventPublisher;

    private AppointmentService appointmentService;

    @BeforeEach
    void setUp() {
        appointmentService = new AppointmentService(
                appointmentRepository,
                doctorServiceClient,
                authServiceClient,
                new AppointmentMapper(),
                appointmentEventPublisher
        );
    }

    @Test
    void getAvailableSlotsExcludesBlockedAndBookedSlots() {
        Long doctorId = 7L;
        LocalDate date = LocalDate.of(2026, 5, 4);
        when(doctorServiceClient.getDoctorSchedule(doctorId, date, date)).thenReturn(schedule(date));

        Appointment booked = new Appointment();
        booked.setStartTime(LocalTime.of(9, 30));
        booked.setEndTime(LocalTime.of(10, 0));
        when(appointmentRepository.findAllByDoctorIdAndAppointmentDateAndStatusOrderByStartTimeAsc(
                doctorId,
                date,
                AppointmentStatus.BOOKED
        )).thenReturn(List.of(booked));

        List<AvailableSlotResponse> slots = appointmentService.getAvailableSlots(doctorId, date);

        assertEquals(1, slots.size());
        assertEquals(LocalTime.of(9, 0), slots.getFirst().startTime());
    }

    @Test
    void createAppointmentBooksAvailableSlotForPatient() {
        Long doctorId = 7L;
        LocalDate date = LocalDate.of(2026, 5, 4);

        when(authServiceClient.getUserByEmail("patient@example.com"))
                .thenReturn(new InternalUserResponse(100L, "Alice Patient", "patient@example.com", "PATIENT", true));
        when(doctorServiceClient.getDoctorSchedule(doctorId, date, date)).thenReturn(schedule(date));
        when(appointmentRepository.findAllByDoctorIdAndAppointmentDateAndStatusOrderByStartTimeAsc(
                doctorId,
                date,
                AppointmentStatus.BOOKED
        )).thenReturn(List.of());
        when(appointmentRepository.existsByDoctorIdAndAppointmentDateAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
                doctorId,
                date,
                AppointmentStatus.BOOKED,
                LocalTime.of(9, 30),
                LocalTime.of(9, 0)
        )).thenReturn(false);
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> {
            Appointment saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        CreateAppointmentRequest request = new CreateAppointmentRequest(
                doctorId,
                date,
                LocalTime.of(9, 0),
                null
        );

        AppointmentResponse response = appointmentService.createAppointment(request, patientAuth());

        assertEquals(1L, response.id());
        assertEquals(100L, response.patientId());
        assertEquals("BOOKED", response.status());
        verify(appointmentEventPublisher).publishCreated(any());
    }

    @Test
    void createAppointmentFailsWhenSlotNotAvailable() {
        Long doctorId = 7L;
        LocalDate date = LocalDate.of(2026, 5, 4);

        when(authServiceClient.getUserByEmail("patient@example.com"))
                .thenReturn(new InternalUserResponse(100L, "Alice Patient", "patient@example.com", "PATIENT", true));
        when(doctorServiceClient.getDoctorSchedule(doctorId, date, date)).thenReturn(schedule(date));
        when(appointmentRepository.findAllByDoctorIdAndAppointmentDateAndStatusOrderByStartTimeAsc(
                doctorId,
                date,
                AppointmentStatus.BOOKED
        )).thenReturn(List.of());

        CreateAppointmentRequest request = new CreateAppointmentRequest(
                doctorId,
                date,
                LocalTime.of(11, 0),
                null
        );

        assertThrows(SlotUnavailableException.class, () -> appointmentService.createAppointment(request, patientAuth()));
    }

    @Test
    void cancelAppointmentRejectsWhenPatientCancelsAnotherPatientAppointment() {
        Appointment appointment = new Appointment();
        appointment.setId(1L);
        appointment.setPatientId(10L);
        appointment.setAppointmentDate(LocalDate.now().plusDays(3));
        appointment.setStartTime(LocalTime.of(10, 0));
        appointment.setEndTime(LocalTime.of(10, 30));
        appointment.setStatus(AppointmentStatus.BOOKED);
        appointment.setCancelledAt(null);

        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));
        when(authServiceClient.getUserByEmail("patient@example.com"))
                .thenReturn(new InternalUserResponse(100L, "Alice Patient", "patient@example.com", "PATIENT", true));

        assertThrows(
                AccessDeniedException.class,
                () -> appointmentService.cancelAppointment(1L, new CancelAppointmentRequest("Cannot attend"), patientAuth())
        );
    }

    @Test
    void cancelAppointmentFailsWhenAlreadyCancelled() {
        Appointment appointment = new Appointment();
        appointment.setId(1L);
        appointment.setPatientId(100L);
        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancelledAt(OffsetDateTime.now());

        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));
        when(authServiceClient.getUserByEmail("patient@example.com"))
                .thenReturn(new InternalUserResponse(100L, "Alice Patient", "patient@example.com", "PATIENT", true));

        assertThrows(
                AppointmentConflictException.class,
                () -> appointmentService.cancelAppointment(1L, new CancelAppointmentRequest("Cannot attend"), patientAuth())
        );
    }

    @Test
    void cancelAppointmentCancelsWhenPatientOwnsAppointment() {
        Appointment appointment = new Appointment();
        appointment.setId(1L);
        appointment.setPatientId(100L);
        appointment.setAppointmentDate(LocalDate.now().plusDays(3));
        appointment.setStartTime(LocalTime.of(10, 0));
        appointment.setEndTime(LocalTime.of(10, 30));
        appointment.setStatus(AppointmentStatus.BOOKED);

        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));
        when(authServiceClient.getUserByEmail("patient@example.com"))
                .thenReturn(new InternalUserResponse(100L, "Alice Patient", "patient@example.com", "PATIENT", true));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppointmentResponse response = appointmentService.cancelAppointment(
                1L,
                new CancelAppointmentRequest("Cannot attend"),
                patientAuth()
        );

        assertEquals("CANCELLED", response.status());
        verify(appointmentRepository).save(any(Appointment.class));
        verify(appointmentEventPublisher).publishCancelled(any());
    }

    @Test
    void cancelAppointmentRejectsWhenInsideCutoffWindow() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-05-10T10:00:00Z"), ZoneOffset.UTC);
        AppointmentService serviceWithFixedClock = new AppointmentService(
                appointmentRepository,
                doctorServiceClient,
                authServiceClient,
                new AppointmentMapper(),
                appointmentEventPublisher,
                24,
                24,
                true,
                fixedClock
        );

        Appointment appointment = new Appointment();
        appointment.setId(77L);
        appointment.setPatientId(100L);
        appointment.setAppointmentDate(LocalDate.of(2026, 5, 11));
        appointment.setStartTime(LocalTime.of(8, 0));
        appointment.setEndTime(LocalTime.of(8, 30));
        appointment.setStatus(AppointmentStatus.BOOKED);

        when(appointmentRepository.findById(77L)).thenReturn(Optional.of(appointment));
        when(authServiceClient.getUserByEmail("patient@example.com"))
                .thenReturn(new InternalUserResponse(100L, "Alice Patient", "patient@example.com", "PATIENT", true));

        AppointmentConflictException exception = assertThrows(
                AppointmentConflictException.class,
                () -> serviceWithFixedClock.cancelAppointment(77L, new CancelAppointmentRequest("Cannot attend"), patientAuth())
        );

        assertTrue(exception.getMessage().contains("less than 24 hours"));
    }

    @Test
    void rescheduleAppointmentRejectsWhenInsideCutoffWindow() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-05-10T10:00:00Z"), ZoneOffset.UTC);
        AppointmentService serviceWithFixedClock = new AppointmentService(
                appointmentRepository,
                doctorServiceClient,
                authServiceClient,
                new AppointmentMapper(),
                appointmentEventPublisher,
                24,
                24,
                true,
                fixedClock
        );

        Appointment appointment = new Appointment();
        appointment.setId(78L);
        appointment.setDoctorId(7L);
        appointment.setPatientId(100L);
        appointment.setAppointmentDate(LocalDate.of(2026, 5, 11));
        appointment.setStartTime(LocalTime.of(8, 0));
        appointment.setEndTime(LocalTime.of(8, 30));
        appointment.setStatus(AppointmentStatus.BOOKED);

        when(appointmentRepository.findById(78L)).thenReturn(Optional.of(appointment));
        when(authServiceClient.getUserByEmail("patient@example.com"))
                .thenReturn(new InternalUserResponse(100L, "Alice Patient", "patient@example.com", "PATIENT", true));

        AppointmentConflictException exception = assertThrows(
                AppointmentConflictException.class,
                () -> serviceWithFixedClock.rescheduleAppointment(
                        78L,
                        new RescheduleAppointmentRequest(LocalDate.of(2026, 5, 12), LocalTime.of(9, 0), null),
                        patientAuth()
                )
        );

        assertTrue(exception.getMessage().contains("less than 24 hours"));
    }

    @Test
    void cancelAppointmentAllowsAdminInsideCutoffWhenBypassEnabled() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-05-10T10:00:00Z"), ZoneOffset.UTC);
        AppointmentService serviceWithFixedClock = new AppointmentService(
                appointmentRepository,
                doctorServiceClient,
                authServiceClient,
                new AppointmentMapper(),
                appointmentEventPublisher,
                24,
                24,
                true,
                fixedClock
        );

        Appointment appointment = new Appointment();
        appointment.setId(79L);
        appointment.setPatientId(100L);
        appointment.setAppointmentDate(LocalDate.of(2026, 5, 11));
        appointment.setStartTime(LocalTime.of(8, 0));
        appointment.setEndTime(LocalTime.of(8, 30));
        appointment.setStatus(AppointmentStatus.BOOKED);

        when(appointmentRepository.findById(79L)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppointmentResponse response = serviceWithFixedClock.cancelAppointment(
                79L,
                new CancelAppointmentRequest("Admin cancel"),
                adminAuth()
        );

        assertEquals("CANCELLED", response.status());
        verify(appointmentEventPublisher).publishCancelled(any());
    }

    @Test
    void getDoctorAgendaIncludesPatientFullName() {
        Appointment appointment = new Appointment();
        appointment.setId(50L);
        appointment.setDoctorId(7L);
        appointment.setPatientId(100L);
        appointment.setAppointmentDate(LocalDate.of(2026, 5, 4));
        appointment.setStartTime(LocalTime.of(9, 0));
        appointment.setEndTime(LocalTime.of(9, 30));
        appointment.setStatus(AppointmentStatus.BOOKED);

        when(doctorServiceClient.getDoctorByOwnerEmail("doctor@example.com"))
                .thenReturn(new com.tomas.medical.appointment.client.dto.InternalDoctorOwnerResponse(7L, "doctor@example.com", true));
        when(appointmentRepository.findAllByDoctorIdAndAppointmentDateBetweenOrderByAppointmentDateAscStartTimeAsc(
                7L,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31)
        )).thenReturn(List.of(appointment));
        when(authServiceClient.getUserById(100L))
                .thenReturn(new InternalUserResponse(100L, "Alice Patient", "patient@example.com", "PATIENT", true));

        List<AppointmentResponse> agenda = appointmentService.getDoctorAgenda(
                7L,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                doctorAuth()
        );

        assertEquals(1, agenda.size());
        assertEquals("Alice Patient", agenda.getFirst().patientFullName());
    }

    @Test
    void getPatientAppointmentsIncludesDoctorFullName() {
        Appointment appointment = new Appointment();
        appointment.setId(88L);
        appointment.setDoctorId(7L);
        appointment.setPatientId(100L);
        appointment.setAppointmentDate(LocalDate.of(2026, 5, 20));
        appointment.setStartTime(LocalTime.of(11, 0));
        appointment.setEndTime(LocalTime.of(11, 30));
        appointment.setStatus(AppointmentStatus.BOOKED);

        when(authServiceClient.getUserByEmail("patient@example.com"))
                .thenReturn(new InternalUserResponse(100L, "Alice Patient", "patient@example.com", "PATIENT", true));
        when(appointmentRepository.findAllByPatientIdOrderByAppointmentDateDescStartTimeDesc(100L))
                .thenReturn(List.of(appointment));
        when(doctorServiceClient.getDoctorById(7L))
                .thenReturn(new InternalDoctorSummaryResponse(7L, "Dr. Juan Perez", true));

        List<AppointmentResponse> response = appointmentService.getPatientAppointments(100L, patientAuth());

        assertEquals(1, response.size());
        assertEquals("Dr. Juan Perez", response.getFirst().doctorFullName());
    }

    @Test
    void rescheduleAppointmentUpdatesSlotAndPublishesEvent() {
        LocalDate currentDate = LocalDate.now().plusDays(2);
        LocalDate newDate = currentDate.plusDays(1);

        Appointment appointment = new Appointment();
        appointment.setId(22L);
        appointment.setDoctorId(7L);
        appointment.setPatientId(100L);
        appointment.setAppointmentDate(currentDate);
        appointment.setStartTime(LocalTime.of(9, 0));
        appointment.setEndTime(LocalTime.of(9, 30));
        appointment.setStatus(AppointmentStatus.BOOKED);
        appointment.setReminderRequestedAt(OffsetDateTime.now().minusHours(2));

        when(appointmentRepository.findById(22L)).thenReturn(Optional.of(appointment));
        when(authServiceClient.getUserByEmail("patient@example.com"))
                .thenReturn(new InternalUserResponse(100L, "Alice Patient", "patient@example.com", "PATIENT", true));
        when(authServiceClient.getUserById(100L))
                .thenReturn(new InternalUserResponse(100L, "Alice Patient", "patient@example.com", "PATIENT", true));
        when(doctorServiceClient.getDoctorById(7L))
                .thenReturn(new InternalDoctorSummaryResponse(7L, "Dr. Juan Perez", true));
        when(doctorServiceClient.getDoctorSchedule(7L, newDate, newDate)).thenReturn(schedule(newDate));
        when(appointmentRepository.findAllByDoctorIdAndAppointmentDateAndStatusOrderByStartTimeAsc(
                7L,
                newDate,
                AppointmentStatus.BOOKED
        )).thenReturn(List.of());
        when(appointmentRepository.existsByDoctorIdAndAppointmentDateAndStatusAndStartTimeLessThanAndEndTimeGreaterThanAndIdNot(
                7L,
                newDate,
                AppointmentStatus.BOOKED,
                LocalTime.of(10, 0),
                LocalTime.of(9, 30),
                22L
        )).thenReturn(false);
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppointmentResponse response = appointmentService.rescheduleAppointment(
                22L,
                new RescheduleAppointmentRequest(newDate, LocalTime.of(9, 30), "Cambio de agenda"),
                patientAuth()
        );

        assertEquals(newDate, response.appointmentDate());
        assertEquals(LocalTime.of(9, 30), response.startTime());
        assertEquals(currentDate, appointment.getPreviousAppointmentDate());
        assertEquals(LocalTime.of(9, 0), appointment.getPreviousStartTime());
        assertTrue(appointment.getRescheduledAt() != null);
        assertNull(appointment.getReminderRequestedAt());
        verify(appointmentEventPublisher).publishRescheduled(any());
    }

    @Test
    void rescheduleAppointmentRejectsWhenPatientReschedulesAnotherPatientAppointment() {
        Appointment appointment = new Appointment();
        appointment.setId(23L);
        appointment.setDoctorId(7L);
        appointment.setPatientId(999L);
        appointment.setAppointmentDate(LocalDate.now().plusDays(2));
        appointment.setStartTime(LocalTime.of(9, 0));
        appointment.setEndTime(LocalTime.of(9, 30));
        appointment.setStatus(AppointmentStatus.BOOKED);

        when(appointmentRepository.findById(23L)).thenReturn(Optional.of(appointment));
        when(authServiceClient.getUserByEmail("patient@example.com"))
                .thenReturn(new InternalUserResponse(100L, "Alice Patient", "patient@example.com", "PATIENT", true));

        assertThrows(
                AccessDeniedException.class,
                () -> appointmentService.rescheduleAppointment(
                        23L,
                        new RescheduleAppointmentRequest(LocalDate.now().plusDays(3), LocalTime.of(9, 30), null),
                        patientAuth()
                )
        );
    }

    @Test
    void rescheduleAppointmentRejectsWhenRequestedSlotMatchesCurrentSlot() {
        LocalDate date = LocalDate.now().plusDays(2);
        Appointment appointment = new Appointment();
        appointment.setId(24L);
        appointment.setDoctorId(7L);
        appointment.setPatientId(100L);
        appointment.setAppointmentDate(date);
        appointment.setStartTime(LocalTime.of(9, 0));
        appointment.setEndTime(LocalTime.of(9, 30));
        appointment.setStatus(AppointmentStatus.BOOKED);

        when(appointmentRepository.findById(24L)).thenReturn(Optional.of(appointment));
        when(authServiceClient.getUserByEmail("patient@example.com"))
                .thenReturn(new InternalUserResponse(100L, "Alice Patient", "patient@example.com", "PATIENT", true));

        assertThrows(
                AppointmentConflictException.class,
                () -> appointmentService.rescheduleAppointment(
                        24L,
                        new RescheduleAppointmentRequest(date, LocalTime.of(9, 0), null),
                        patientAuth()
                )
        );
    }

    @Test
    void rescheduleAppointmentRejectsWhenAppointmentAlreadyStarted() {
        LocalDate today = LocalDate.now();
        LocalTime startedTime = LocalTime.now().minusMinutes(5);
        Appointment appointment = new Appointment();
        appointment.setId(25L);
        appointment.setDoctorId(7L);
        appointment.setPatientId(100L);
        appointment.setAppointmentDate(today);
        appointment.setStartTime(startedTime);
        appointment.setEndTime(startedTime.plusMinutes(30));
        appointment.setStatus(AppointmentStatus.BOOKED);

        when(appointmentRepository.findById(25L)).thenReturn(Optional.of(appointment));
        when(authServiceClient.getUserByEmail("patient@example.com"))
                .thenReturn(new InternalUserResponse(100L, "Alice Patient", "patient@example.com", "PATIENT", true));

        assertThrows(
                AppointmentConflictException.class,
                () -> appointmentService.rescheduleAppointment(
                        25L,
                        new RescheduleAppointmentRequest(LocalDate.now().plusDays(1), LocalTime.of(9, 0), null),
                        patientAuth()
                )
        );
    }

    @Test
    void rescheduleAppointmentRejectsWhenRequestedSlotIsAlreadyBooked() {
        LocalDate currentDate = LocalDate.now().plusDays(2);
        LocalDate newDate = currentDate.plusDays(1);

        Appointment appointment = new Appointment();
        appointment.setId(26L);
        appointment.setDoctorId(7L);
        appointment.setPatientId(100L);
        appointment.setAppointmentDate(currentDate);
        appointment.setStartTime(LocalTime.of(9, 0));
        appointment.setEndTime(LocalTime.of(9, 30));
        appointment.setStatus(AppointmentStatus.BOOKED);

        when(appointmentRepository.findById(26L)).thenReturn(Optional.of(appointment));
        when(authServiceClient.getUserByEmail("patient@example.com"))
                .thenReturn(new InternalUserResponse(100L, "Alice Patient", "patient@example.com", "PATIENT", true));
        when(doctorServiceClient.getDoctorSchedule(7L, newDate, newDate)).thenReturn(schedule(newDate));
        when(appointmentRepository.findAllByDoctorIdAndAppointmentDateAndStatusOrderByStartTimeAsc(
                7L,
                newDate,
                AppointmentStatus.BOOKED
        )).thenReturn(List.of());
        when(appointmentRepository.existsByDoctorIdAndAppointmentDateAndStatusAndStartTimeLessThanAndEndTimeGreaterThanAndIdNot(
                7L,
                newDate,
                AppointmentStatus.BOOKED,
                LocalTime.of(10, 0),
                LocalTime.of(9, 30),
                26L
        )).thenReturn(true);

        assertThrows(
                AppointmentConflictException.class,
                () -> appointmentService.rescheduleAppointment(
                        26L,
                        new RescheduleAppointmentRequest(newDate, LocalTime.of(9, 30), null),
                        patientAuth()
                )
        );
    }

    private DoctorScheduleResponse schedule(LocalDate date) {
        DoctorScheduleResponse.DoctorScheduleWindowResponse window =
                new DoctorScheduleResponse.DoctorScheduleWindowResponse(
                        1L,
                        LocalTime.of(9, 0),
                        LocalTime.of(10, 0),
                        30
                );

        DoctorScheduleResponse.DoctorScheduleBlockedSlotResponse blocked =
                new DoctorScheduleResponse.DoctorScheduleBlockedSlotResponse(
                        9L,
                        LocalTime.of(10, 0),
                        LocalTime.of(10, 30)
                );

        DoctorScheduleResponse.DoctorScheduleDayResponse day =
                new DoctorScheduleResponse.DoctorScheduleDayResponse(
                        date,
                        DayOfWeek.MONDAY,
                        List.of(window),
                        List.of(blocked)
                );

        return new DoctorScheduleResponse(
                7L,
                date,
                date,
                List.of(),
                List.of(day)
        );
    }

    private UsernamePasswordAuthenticationToken patientAuth() {
        return new UsernamePasswordAuthenticationToken(
                "patient@example.com",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_PATIENT"))
        );
    }

    private UsernamePasswordAuthenticationToken doctorAuth() {
        return new UsernamePasswordAuthenticationToken(
                "doctor@example.com",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_DOCTOR"))
        );
    }

    private UsernamePasswordAuthenticationToken adminAuth() {
        return new UsernamePasswordAuthenticationToken(
                "admin@example.com",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
    }
}
