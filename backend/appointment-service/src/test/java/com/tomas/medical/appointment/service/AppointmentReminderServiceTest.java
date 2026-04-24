package com.tomas.medical.appointment.service;

import com.tomas.medical.appointment.client.AuthServiceClient;
import com.tomas.medical.appointment.client.DoctorServiceClient;
import com.tomas.medical.appointment.client.dto.InternalDoctorSummaryResponse;
import com.tomas.medical.appointment.client.dto.InternalUserResponse;
import com.tomas.medical.appointment.entity.Appointment;
import com.tomas.medical.appointment.enumtype.AppointmentStatus;
import com.tomas.medical.appointment.messaging.producer.AppointmentEventPublisher;
import com.tomas.medical.appointment.repository.AppointmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentReminderServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private AppointmentEventPublisher appointmentEventPublisher;

    @Mock
    private AuthServiceClient authServiceClient;

    @Mock
    private DoctorServiceClient doctorServiceClient;

    private AppointmentReminderService appointmentReminderService;

    @BeforeEach
    void setUp() {
        appointmentReminderService = new AppointmentReminderService(
                appointmentRepository,
                appointmentEventPublisher,
                authServiceClient,
                doctorServiceClient,
                24
        );
    }

    @Test
    void dispatchReminderRequestsPublishesAndMarksAppointmentInsideWindow() {
        Appointment appointment = bookedAt(LocalDateTime.now().plusHours(3));

        when(appointmentRepository.findAllByStatusAndReminderRequestedAtIsNullAndAppointmentDateBetweenOrderByAppointmentDateAscStartTimeAsc(
                any(),
                any(),
                any()
        )).thenReturn(List.of(appointment));
        when(authServiceClient.getUserById(100L))
                .thenReturn(new InternalUserResponse(100L, "Patient", "patient@example.com", "PATIENT", true));
        when(doctorServiceClient.getDoctorById(7L))
                .thenReturn(new InternalDoctorSummaryResponse(7L, "Dr. Gomez", true));

        int published = appointmentReminderService.dispatchReminderRequests();

        assertEquals(1, published);
        assertNotNull(appointment.getReminderRequestedAt());
        verify(appointmentEventPublisher).publishReminderRequested(any());
    }

    @Test
    void dispatchReminderRequestsSkipsAlreadyMarkedAppointment() {
        Appointment appointment = bookedAt(LocalDateTime.now().plusHours(2));
        appointment.setReminderRequestedAt(OffsetDateTime.now().minusMinutes(1));

        when(appointmentRepository.findAllByStatusAndReminderRequestedAtIsNullAndAppointmentDateBetweenOrderByAppointmentDateAscStartTimeAsc(
                any(),
                any(),
                any()
        )).thenReturn(List.of(appointment));

        int published = appointmentReminderService.dispatchReminderRequests();

        assertEquals(0, published);
        verify(appointmentEventPublisher, never()).publishReminderRequested(any());
    }

    @Test
    void dispatchReminderRequestsDoesNotDuplicateReminderAcrossRuns() {
        Appointment appointment = bookedAt(LocalDateTime.now().plusHours(3));

        when(appointmentRepository.findAllByStatusAndReminderRequestedAtIsNullAndAppointmentDateBetweenOrderByAppointmentDateAscStartTimeAsc(
                any(),
                any(),
                any()
        )).thenReturn(List.of(appointment));
        when(authServiceClient.getUserById(100L))
                .thenReturn(new InternalUserResponse(100L, "Patient", "patient@example.com", "PATIENT", true));
        when(doctorServiceClient.getDoctorById(7L))
                .thenReturn(new InternalDoctorSummaryResponse(7L, "Dr. Gomez", true));

        int firstRun = appointmentReminderService.dispatchReminderRequests();
        int secondRun = appointmentReminderService.dispatchReminderRequests();

        assertEquals(1, firstRun);
        assertEquals(0, secondRun);
        verify(appointmentEventPublisher).publishReminderRequested(any());
    }

    @Test
    void dispatchReminderRequestsSkipsAppointmentOutsideWindow() {
        Appointment appointment = bookedAt(LocalDateTime.now().plusHours(30));

        when(appointmentRepository.findAllByStatusAndReminderRequestedAtIsNullAndAppointmentDateBetweenOrderByAppointmentDateAscStartTimeAsc(
                any(),
                any(),
                any()
        )).thenReturn(List.of(appointment));

        int published = appointmentReminderService.dispatchReminderRequests();

        assertEquals(0, published);
        assertNull(appointment.getReminderRequestedAt());
        verify(appointmentEventPublisher, never()).publishReminderRequested(any());
    }

    @Test
    void dispatchReminderRequestsSkipsPastAppointment() {
        Appointment appointment = bookedAt(LocalDateTime.now().minusMinutes(30));

        when(appointmentRepository.findAllByStatusAndReminderRequestedAtIsNullAndAppointmentDateBetweenOrderByAppointmentDateAscStartTimeAsc(
                any(),
                any(),
                any()
        )).thenReturn(List.of(appointment));

        int published = appointmentReminderService.dispatchReminderRequests();

        assertEquals(0, published);
        assertNull(appointment.getReminderRequestedAt());
        verify(appointmentEventPublisher, never()).publishReminderRequested(any());
    }

    private Appointment bookedAt(LocalDateTime startDateTime) {
        Appointment appointment = new Appointment();
        appointment.setId(10L);
        appointment.setDoctorId(7L);
        appointment.setPatientId(100L);
        appointment.setAppointmentDate(LocalDate.from(startDateTime));
        appointment.setStartTime(LocalTime.from(startDateTime));
        appointment.setEndTime(LocalTime.from(startDateTime.plusMinutes(30)));
        appointment.setStatus(AppointmentStatus.BOOKED);
        return appointment;
    }
}
