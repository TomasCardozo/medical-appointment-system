package com.tomas.medical.appointment.service;

import com.tomas.medical.appointment.client.AuthServiceClient;
import com.tomas.medical.appointment.client.DoctorServiceClient;
import com.tomas.medical.appointment.client.dto.DoctorScheduleResponse;
import com.tomas.medical.appointment.client.dto.InternalDoctorOwnerResponse;
import com.tomas.medical.appointment.client.dto.InternalDoctorSummaryResponse;
import com.tomas.medical.appointment.client.dto.InternalUserResponse;
import com.tomas.medical.appointment.dto.event.AppointmentCancelledEvent;
import com.tomas.medical.appointment.dto.event.AppointmentCreatedEvent;
import com.tomas.medical.appointment.dto.event.AppointmentRescheduledEvent;
import com.tomas.medical.appointment.dto.request.CancelAppointmentRequest;
import com.tomas.medical.appointment.dto.request.CreateAppointmentRequest;
import com.tomas.medical.appointment.dto.request.RescheduleAppointmentRequest;
import com.tomas.medical.appointment.dto.response.AppointmentResponse;
import com.tomas.medical.appointment.dto.response.AvailableSlotResponse;
import com.tomas.medical.appointment.entity.Appointment;
import com.tomas.medical.appointment.enumtype.AppointmentStatus;
import com.tomas.medical.appointment.exception.AppointmentConflictException;
import com.tomas.medical.appointment.exception.AppointmentNotFoundException;
import com.tomas.medical.appointment.exception.DoctorNotFoundException;
import com.tomas.medical.appointment.exception.ExternalServiceException;
import com.tomas.medical.appointment.exception.SlotUnavailableException;
import com.tomas.medical.appointment.mapper.AppointmentMapper;
import com.tomas.medical.appointment.messaging.producer.AppointmentEventPublisher;
import com.tomas.medical.appointment.repository.AppointmentRepository;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AppointmentService {

    private static final int MAX_AGENDA_RANGE_DAYS = 31;
    private static final Logger log = LoggerFactory.getLogger(AppointmentService.class);

    private final AppointmentRepository appointmentRepository;
    private final DoctorServiceClient doctorServiceClient;
    private final AuthServiceClient authServiceClient;
    private final AppointmentMapper appointmentMapper;
    private final AppointmentEventPublisher appointmentEventPublisher;
    private final long cancelCutoffHours;
    private final long rescheduleCutoffHours;
    private final boolean adminBypassCutoffs;
    private final Clock clock;

    AppointmentService(AppointmentRepository appointmentRepository,
                       DoctorServiceClient doctorServiceClient,
                       AuthServiceClient authServiceClient,
                       AppointmentMapper appointmentMapper,
                       AppointmentEventPublisher appointmentEventPublisher) {
        this(
                appointmentRepository,
                doctorServiceClient,
                authServiceClient,
                appointmentMapper,
                appointmentEventPublisher,
                24,
                24,
                true,
                Clock.systemDefaultZone()
        );
    }

    @Autowired
    public AppointmentService(AppointmentRepository appointmentRepository,
                              DoctorServiceClient doctorServiceClient,
                              AuthServiceClient authServiceClient,
                              AppointmentMapper appointmentMapper,
                              AppointmentEventPublisher appointmentEventPublisher,
                              @Value("${app.rules.cancel-cutoff-hours:24}") long cancelCutoffHours,
                              @Value("${app.rules.reschedule-cutoff-hours:24}") long rescheduleCutoffHours,
                              @Value("${app.rules.admin-bypass-cutoffs:true}") boolean adminBypassCutoffs) {
        this(
                appointmentRepository,
                doctorServiceClient,
                authServiceClient,
                appointmentMapper,
                appointmentEventPublisher,
                cancelCutoffHours,
                rescheduleCutoffHours,
                adminBypassCutoffs,
                Clock.systemDefaultZone()
        );
    }

    AppointmentService(AppointmentRepository appointmentRepository,
                       DoctorServiceClient doctorServiceClient,
                       AuthServiceClient authServiceClient,
                       AppointmentMapper appointmentMapper,
                       AppointmentEventPublisher appointmentEventPublisher,
                       long cancelCutoffHours,
                       long rescheduleCutoffHours,
                       boolean adminBypassCutoffs,
                       Clock clock) {
        this.appointmentRepository = appointmentRepository;
        this.doctorServiceClient = doctorServiceClient;
        this.authServiceClient = authServiceClient;
        this.appointmentMapper = appointmentMapper;
        this.appointmentEventPublisher = appointmentEventPublisher;
        if (cancelCutoffHours < 0 || rescheduleCutoffHours < 0) {
            throw new IllegalArgumentException("Cutoff hours must be zero or greater");
        }
        this.cancelCutoffHours = cancelCutoffHours;
        this.rescheduleCutoffHours = rescheduleCutoffHours;
        this.adminBypassCutoffs = adminBypassCutoffs;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<AvailableSlotResponse> getAvailableSlots(Long doctorId, LocalDate date) {
        validateDoctorIdAndDate(doctorId, date);

        DoctorScheduleResponse schedule = fetchDoctorSchedule(doctorId, date, date);
        if (schedule.days() == null || schedule.days().isEmpty()) {
            return List.of();
        }

        DoctorScheduleResponse.DoctorScheduleDayResponse day = schedule.days().getFirst();
        List<SlotRange> slotsFromSchedule = buildSlotsFromDay(doctorId, day);

        List<Appointment> bookedAppointments = appointmentRepository
                .findAllByDoctorIdAndAppointmentDateAndStatusOrderByStartTimeAsc(
                        doctorId,
                        date,
                        AppointmentStatus.BOOKED
                );

        List<AvailableSlotResponse> availableSlots = new ArrayList<>();
        for (SlotRange slot : slotsFromSchedule) {
            boolean occupied = bookedAppointments.stream().anyMatch(appointment -> overlaps(
                    slot.startTime(),
                    slot.endTime(),
                    appointment.getStartTime(),
                    appointment.getEndTime()
            ));

            if (!occupied) {
                availableSlots.add(new AvailableSlotResponse(
                        doctorId,
                        date,
                        slot.startTime(),
                        slot.endTime(),
                        slot.slotDurationMinutes()
                ));
            }
        }

        return availableSlots;
    }

    @Transactional
    public AppointmentResponse createAppointment(CreateAppointmentRequest request, Authentication authentication) {
        if (authentication == null) {
            throw new AccessDeniedException("Authentication is required");
        }

        InternalUserResponse patient = resolvePatientForCreate(authentication, request.patientId());
        List<AvailableSlotResponse> availableSlots = getAvailableSlots(request.doctorId(), request.appointmentDate());

        AvailableSlotResponse selectedSlot = availableSlots.stream()
                .filter(slot -> slot.startTime().equals(request.startTime()))
                .findFirst()
                .orElseThrow(() -> new SlotUnavailableException("Requested slot is not available"));

        boolean overlapping = appointmentRepository
                .existsByDoctorIdAndAppointmentDateAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
                        request.doctorId(),
                        request.appointmentDate(),
                        AppointmentStatus.BOOKED,
                        selectedSlot.endTime(),
                        selectedSlot.startTime()
                );

        if (overlapping) {
            throw new AppointmentConflictException("Slot already booked for this doctor");
        }

        Appointment appointment = new Appointment();
        appointment.setDoctorId(request.doctorId());
        appointment.setPatientId(patient.id());
        appointment.setAppointmentDate(request.appointmentDate());
        appointment.setStartTime(selectedSlot.startTime());
        appointment.setEndTime(selectedSlot.endTime());
        appointment.setStatus(AppointmentStatus.BOOKED);

        Appointment saved = appointmentRepository.save(appointment);
        publishCreatedEvent(saved, patient);
        return appointmentMapper.toResponse(saved);
    }

    @Transactional
    public AppointmentResponse cancelAppointment(Long appointmentId,
                                                 CancelAppointmentRequest request,
                                                 Authentication authentication) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new AppointmentNotFoundException(appointmentId));

        assertCanCancelAppointment(authentication, appointment);

        if (appointment.getStatus() != AppointmentStatus.BOOKED) {
            throw new AppointmentConflictException("Only booked appointments can be cancelled");
        }

        validateSlotNotInPast(
                appointment.getAppointmentDate(),
                appointment.getStartTime(),
                "Cannot cancel an appointment that has already started"
        );
        validateActionCutoff(authentication, appointment, cancelCutoffHours, "cancel");

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancellationReason(request.cancellationReason());
        appointment.setCancelledAt(OffsetDateTime.now());

        Appointment saved = appointmentRepository.save(appointment);
        publishCancelledEvent(saved);
        return appointmentMapper.toResponse(saved);
    }

    @Transactional
    public AppointmentResponse rescheduleAppointment(Long appointmentId,
                                                     RescheduleAppointmentRequest request,
                                                     Authentication authentication) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new AppointmentNotFoundException(appointmentId));

        assertCanRescheduleAppointment(authentication, appointment);

        if (appointment.getStatus() != AppointmentStatus.BOOKED) {
            throw new AppointmentConflictException("Only booked appointments can be rescheduled");
        }

        validateSlotNotInPast(appointment.getAppointmentDate(), appointment.getStartTime(),
                "Cannot reschedule an appointment that has already started");
        validateActionCutoff(authentication, appointment, rescheduleCutoffHours, "reschedule");

        if (appointment.getAppointmentDate().equals(request.appointmentDate())
            && appointment.getStartTime().equals(request.startTime())) {
            throw new AppointmentConflictException("Requested slot matches current appointment slot");
        }

        List<AvailableSlotResponse> availableSlots = getAvailableSlots(appointment.getDoctorId(), request.appointmentDate());
        AvailableSlotResponse selectedSlot = availableSlots.stream()
                .filter(slot -> slot.startTime().equals(request.startTime()))
                .findFirst()
                .orElseThrow(() -> new SlotUnavailableException("Requested slot is not available"));

        validateSlotNotInPast(selectedSlot.date(), selectedSlot.startTime(), "Cannot reschedule to a past slot");

        boolean overlapping = appointmentRepository
                .existsByDoctorIdAndAppointmentDateAndStatusAndStartTimeLessThanAndEndTimeGreaterThanAndIdNot(
                        appointment.getDoctorId(),
                        selectedSlot.date(),
                        AppointmentStatus.BOOKED,
                        selectedSlot.endTime(),
                        selectedSlot.startTime(),
                        appointment.getId()
                );

        if (overlapping) {
            throw new AppointmentConflictException("Slot already booked for this doctor");
        }

        appointment.setPreviousAppointmentDate(appointment.getAppointmentDate());
        appointment.setPreviousStartTime(appointment.getStartTime());
        appointment.setPreviousEndTime(appointment.getEndTime());
        appointment.setAppointmentDate(selectedSlot.date());
        appointment.setStartTime(selectedSlot.startTime());
        appointment.setEndTime(selectedSlot.endTime());
        appointment.setRescheduleReason(request.rescheduleReason());
        appointment.setRescheduledAt(OffsetDateTime.now());
        appointment.setReminderRequestedAt(null);

        Appointment saved = appointmentRepository.save(appointment);
        publishRescheduledEvent(saved);
        return appointmentMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> getPatientAppointments(Long patientId, Authentication authentication) {
        assertCanAccessPatientAppointments(authentication, patientId);

        List<Appointment> appointments = appointmentRepository
                .findAllByPatientIdOrderByAppointmentDateDescStartTimeDesc(patientId);

        Map<Long, String> doctorNamesById = resolveDoctorNamesById(appointments);

        return appointments.stream()
                .map(appointment -> appointmentMapper.toResponse(
                        appointment,
                        doctorNamesById.get(appointment.getDoctorId()),
                        null
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> getDoctorAppointments(Long doctorId, Authentication authentication) {
        assertCanAccessDoctorAgenda(authentication, doctorId);

        return appointmentRepository.findAllByDoctorIdOrderByAppointmentDateDescStartTimeDesc(doctorId).stream()
                .map(appointmentMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> getDoctorAgenda(Long doctorId,
                                                     LocalDate from,
                                                     LocalDate to,
                                                     Authentication authentication) {
        assertCanAccessDoctorAgenda(authentication, doctorId);
        validateAgendaRange(from, to);

        List<Appointment> appointments = appointmentRepository
                .findAllByDoctorIdAndAppointmentDateBetweenOrderByAppointmentDateAscStartTimeAsc(
                        doctorId,
                        from,
                        to
                );

        Map<Long, String> patientNamesById = resolvePatientNamesById(appointments);

        return appointments.stream()
                .map(appointment -> appointmentMapper.toResponse(
                        appointment,
                        null,
                        patientNamesById.get(appointment.getPatientId())
                ))
                .toList();
    }

    private Map<Long, String> resolveDoctorNamesById(List<Appointment> appointments) {
        Map<Long, String> doctorNamesById = new HashMap<>();

        for (Appointment appointment : appointments) {
            Long doctorId = appointment.getDoctorId();
            if (doctorId == null || doctorNamesById.containsKey(doctorId)) {
                continue;
            }

            doctorNamesById.put(doctorId, resolveDoctorNameById(doctorId));
        }

        return doctorNamesById;
    }

    private void assertCanCancelAppointment(Authentication authentication, Appointment appointment) {
        if (authentication == null) {
            throw new AccessDeniedException("Authentication is required");
        }

        if (hasRole(authentication, "ADMIN")) {
            return;
        }

        if (!hasRole(authentication, "PATIENT")) {
            throw new AccessDeniedException("Only patients can cancel appointments");
        }

        Long authenticatedPatientId = resolveUserByEmail(authentication.getName()).id();
        if (!authenticatedPatientId.equals(appointment.getPatientId())) {
            throw new AccessDeniedException("You can only cancel your own appointments");
        }
    }

    private void assertCanRescheduleAppointment(Authentication authentication, Appointment appointment) {
        if (authentication == null) {
            throw new AccessDeniedException("Authentication is required");
        }

        if (hasRole(authentication, "ADMIN")) {
            return;
        }

        if (!hasRole(authentication, "PATIENT")) {
            throw new AccessDeniedException("Only patients can reschedule appointments");
        }

        Long authenticatedPatientId = resolveUserByEmail(authentication.getName()).id();
        if (!authenticatedPatientId.equals(appointment.getPatientId())) {
            throw new AccessDeniedException("You can only reschedule your own appointments");
        }
    }

    private void assertCanAccessPatientAppointments(Authentication authentication, Long patientId) {
        if (authentication == null) {
            throw new AccessDeniedException("Authentication is required");
        }

        if (hasRole(authentication, "ADMIN")) {
            return;
        }

        if (!hasRole(authentication, "PATIENT")) {
            throw new AccessDeniedException("Only patients can access patient appointments");
        }

        Long authenticatedPatientId = resolveUserByEmail(authentication.getName()).id();
        if (!authenticatedPatientId.equals(patientId)) {
            throw new AccessDeniedException("You are not allowed to access another patient's appointments");
        }
    }

    private void assertCanAccessDoctorAgenda(Authentication authentication, Long doctorId) {
        if (authentication == null) {
            throw new AccessDeniedException("Authentication is required");
        }

        if (hasRole(authentication, "ADMIN")) {
            return;
        }

        if (!hasRole(authentication, "DOCTOR")) {
            throw new AccessDeniedException("Only doctors can access doctor agenda");
        }

        InternalDoctorOwnerResponse doctorOwner = resolveDoctorByOwnerEmail(authentication.getName());
        if (!doctorOwner.id().equals(doctorId)) {
            throw new AccessDeniedException("You are not allowed to access another doctor's agenda");
        }
    }

    private InternalUserResponse resolvePatientForCreate(Authentication authentication, Long requestedPatientId) {
        if (hasRole(authentication, "ADMIN")) {
            if (requestedPatientId == null) {
                throw new IllegalArgumentException("patientId is required when role is ADMIN");
            }
            return resolveUserById(requestedPatientId);
        }

        if (!hasRole(authentication, "PATIENT")) {
            throw new AccessDeniedException("Only patients can create appointments");
        }

        return resolveUserByEmail(authentication.getName());
    }

    private void publishCreatedEvent(Appointment appointment, InternalUserResponse patient) {
        AppointmentCreatedEvent event = new AppointmentCreatedEvent(
                appointment.getId(),
                appointment.getDoctorId(),
                safeResolveDoctorNameById(appointment.getDoctorId()),
                patient.id(),
                patient.fullName(),
                patient.email(),
                appointment.getAppointmentDate(),
                appointment.getStartTime(),
                appointment.getEndTime(),
                OffsetDateTime.now()
        );

        try {
            appointmentEventPublisher.publishCreated(event);
        } catch (RuntimeException ex) {
            log.warn("Failed to publish appointment.created event for appointmentId={}", appointment.getId(), ex);
        }
    }

    private void publishCancelledEvent(Appointment appointment) {
        InternalUserResponse patient = safeResolveUserById(appointment.getPatientId());
        AppointmentCancelledEvent event = new AppointmentCancelledEvent(
                appointment.getId(),
                appointment.getDoctorId(),
                safeResolveDoctorNameById(appointment.getDoctorId()),
                appointment.getPatientId(),
                patient != null ? patient.fullName() : null,
                patient != null ? patient.email() : null,
                appointment.getAppointmentDate(),
                appointment.getStartTime(),
                appointment.getEndTime(),
                appointment.getCancellationReason(),
                appointment.getCancelledAt(),
                OffsetDateTime.now()
        );

        try {
            appointmentEventPublisher.publishCancelled(event);
        } catch (RuntimeException ex) {
            log.warn("Failed to publish appointment.cancelled event for appointmentId={}", appointment.getId(), ex);
        }
    }

    private void publishRescheduledEvent(Appointment appointment) {
        InternalUserResponse patient = safeResolveUserById(appointment.getPatientId());
        AppointmentRescheduledEvent event = new AppointmentRescheduledEvent(
                appointment.getId(),
                appointment.getDoctorId(),
                safeResolveDoctorNameById(appointment.getDoctorId()),
                appointment.getPatientId(),
                patient != null ? patient.fullName() : null,
                patient != null ? patient.email() : null,
                appointment.getPreviousAppointmentDate(),
                appointment.getPreviousStartTime(),
                appointment.getPreviousEndTime(),
                appointment.getAppointmentDate(),
                appointment.getStartTime(),
                appointment.getEndTime(),
                appointment.getRescheduleReason(),
                appointment.getRescheduledAt(),
                OffsetDateTime.now()
        );

        try {
            appointmentEventPublisher.publishRescheduled(event);
        } catch (RuntimeException ex) {
            log.warn("Failed to publish appointment.rescheduled event for appointmentId={}", appointment.getId(), ex);
        }
    }

    private InternalUserResponse resolveUserByEmail(String email) {
        try {
            return authServiceClient.getUserByEmail(normalizeEmail(email));
        } catch (FeignException.NotFound ex) {
            throw new AccessDeniedException("Authenticated user not found");
        } catch (FeignException ex) {
            throw new ExternalServiceException("Failed to fetch user identity from auth-service", ex);
        }
    }

    private InternalUserResponse resolveUserById(Long userId) {
        try {
            return authServiceClient.getUserById(userId);
        } catch (FeignException.NotFound ex) {
            throw new AccessDeniedException("User not found");
        } catch (FeignException ex) {
            throw new ExternalServiceException("Failed to fetch user identity from auth-service", ex);
        }
    }

    private InternalUserResponse safeResolveUserById(Long userId) {
        try {
            return resolveUserById(userId);
        } catch (RuntimeException ex) {
            log.warn("Could not enrich patient identity for userId={}", userId, ex);
            return null;
        }
    }

    private String safeResolveDoctorNameById(Long doctorId) {
        try {
            return resolveDoctorNameById(doctorId);
        } catch (RuntimeException ex) {
            log.warn("Could not enrich doctor identity for doctorId={}", doctorId, ex);
            return null;
        }
    }

    private Map<Long, String> resolvePatientNamesById(List<Appointment> appointments) {
        Map<Long, String> patientNamesById = new HashMap<>();

        for (Appointment appointment : appointments) {
            Long patientId = appointment.getPatientId();
            if (patientId == null || patientNamesById.containsKey(patientId)) {
                continue;
            }

            patientNamesById.put(patientId, resolvePatientNameById(patientId));
        }

        return patientNamesById;
    }

    private String resolvePatientNameById(Long patientId) {
        try {
            return authServiceClient.getUserById(patientId).fullName();
        } catch (FeignException.NotFound ex) {
            return null;
        } catch (FeignException ex) {
            throw new ExternalServiceException("Failed to fetch user identity from auth-service", ex);
        }
    }

    private String resolveDoctorNameById(Long doctorId) {
        try {
            InternalDoctorSummaryResponse doctor = doctorServiceClient.getDoctorById(doctorId);
            if (doctor == null) {
                return null;
            }
            return doctor.fullName();
        } catch (FeignException.NotFound ex) {
            return null;
        } catch (FeignException ex) {
            throw new ExternalServiceException("Failed to fetch doctor identity from doctor-service", ex);
        }
    }

    private InternalDoctorOwnerResponse resolveDoctorByOwnerEmail(String email) {
        try {
            return doctorServiceClient.getDoctorByOwnerEmail(normalizeEmail(email));
        } catch (FeignException.NotFound ex) {
            throw new AccessDeniedException("Doctor profile not found for authenticated user");
        } catch (FeignException ex) {
            throw new ExternalServiceException("Failed to fetch doctor owner from doctor-service", ex);
        }
    }

    private DoctorScheduleResponse fetchDoctorSchedule(Long doctorId, LocalDate from, LocalDate to) {
        try {
            return doctorServiceClient.getDoctorSchedule(doctorId, from, to);
        } catch (FeignException.NotFound ex) {
            throw new DoctorNotFoundException(doctorId);
        } catch (FeignException ex) {
            throw new ExternalServiceException("Failed to fetch doctor schedule from doctor-service", ex);
        }
    }

    private List<SlotRange> buildSlotsFromDay(Long doctorId, DoctorScheduleResponse.DoctorScheduleDayResponse day) {
        List<SlotRange> slots = new ArrayList<>();
        if (day.availableWindows() == null || day.availableWindows().isEmpty()) {
            return slots;
        }

        for (DoctorScheduleResponse.DoctorScheduleWindowResponse window : day.availableWindows()) {
            LocalTime cursor = window.startTime();
            int duration = window.slotDurationMinutes();

            while (!cursor.plusMinutes(duration).isAfter(window.endTime())) {
                LocalTime endTime = cursor.plusMinutes(duration);
                boolean blocked = isBlocked(day, cursor, endTime);
                if (!blocked) {
                    slots.add(new SlotRange(doctorId, day.date(), cursor, endTime, duration));
                }
                cursor = cursor.plusMinutes(duration);
            }
        }

        return slots;
    }

    private boolean isBlocked(DoctorScheduleResponse.DoctorScheduleDayResponse day, LocalTime startTime, LocalTime endTime) {
        if (day.blockedSlots() == null || day.blockedSlots().isEmpty()) {
            return false;
        }

        return day.blockedSlots().stream().anyMatch(blocked -> overlaps(
                startTime,
                endTime,
                blocked.startTime(),
                blocked.endTime()
        ));
    }

    private boolean overlaps(LocalTime start1, LocalTime end1, LocalTime start2, LocalTime end2) {
        return start1.isBefore(end2) && end1.isAfter(start2);
    }

    private boolean hasRole(Authentication authentication, String role) {
        String expected = "ROLE_" + role;
        return authentication.getAuthorities().stream().anyMatch(authority -> expected.equals(authority.getAuthority()));
    }

    private void validateDoctorIdAndDate(Long doctorId, LocalDate date) {
        if (doctorId == null || date == null) {
            throw new IllegalArgumentException("doctorId and date are required");
        }
    }

    private void validateAgendaRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("from and to are required");
        }

        if (from.isAfter(to)) {
            throw new IllegalArgumentException("from must be before or equal to to");
        }

        if (from.plusDays(MAX_AGENDA_RANGE_DAYS - 1L).isBefore(to)) {
            throw new IllegalArgumentException("Date range cannot exceed " + MAX_AGENDA_RANGE_DAYS + " days");
        }
    }

    private void validateSlotNotInPast(LocalDate appointmentDate, LocalTime startTime, String message) {
        LocalDateTime slotDateTime = LocalDateTime.of(appointmentDate, startTime);
        if (!slotDateTime.isAfter(LocalDateTime.now(clock))) {
            throw new AppointmentConflictException(message);
        }
    }

    private void validateActionCutoff(Authentication authentication,
                                      Appointment appointment,
                                      long cutoffHours,
                                      String action) {
        if (adminBypassCutoffs && hasRole(authentication, "ADMIN")) {
            return;
        }

        LocalDateTime appointmentStart = LocalDateTime.of(appointment.getAppointmentDate(), appointment.getStartTime());
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime cutoffLimit = appointmentStart.minusHours(cutoffHours);

        if (!now.isBefore(cutoffLimit)) {
            throw new AppointmentConflictException(
                    "Cannot " + action + " an appointment less than " + cutoffHours + " hours before start time"
            );
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private record SlotRange(
            Long doctorId,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            Integer slotDurationMinutes
    ) {
    }
}
