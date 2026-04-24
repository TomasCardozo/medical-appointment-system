package com.tomas.medical.doctor.service;

import com.tomas.medical.doctor.dto.request.CreateBlockedSlotRequest;
import com.tomas.medical.doctor.dto.request.CreateDoctorAvailabilityRequest;
import com.tomas.medical.doctor.dto.request.UpdateDoctorAvailabilityRequest;
import com.tomas.medical.doctor.dto.response.BlockedSlotResponse;
import com.tomas.medical.doctor.dto.response.DoctorAvailabilityResponse;
import com.tomas.medical.doctor.dto.response.DoctorScheduleResponse;
import com.tomas.medical.doctor.entity.BlockedSlot;
import com.tomas.medical.doctor.entity.DoctorAvailability;
import com.tomas.medical.doctor.entity.DoctorProfile;
import com.tomas.medical.doctor.exception.BlockedSlotConflictException;
import com.tomas.medical.doctor.exception.DoctorAvailabilityConflictException;
import com.tomas.medical.doctor.exception.DoctorAvailabilityNotFoundException;
import com.tomas.medical.doctor.exception.DoctorProfileNotFoundException;
import com.tomas.medical.doctor.mapper.DoctorScheduleMapper;
import com.tomas.medical.doctor.repository.BlockedSlotRepository;
import com.tomas.medical.doctor.repository.DoctorAvailabilityRepository;
import com.tomas.medical.doctor.repository.DoctorProfileRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DoctorAvailabilityService {

    private static final int MAX_SCHEDULE_RANGE_DAYS = 31;

    private final DoctorProfileRepository doctorProfileRepository;
    private final DoctorAvailabilityRepository doctorAvailabilityRepository;
    private final BlockedSlotRepository blockedSlotRepository;
    private final DoctorScheduleMapper doctorScheduleMapper;

    public DoctorAvailabilityService(DoctorProfileRepository doctorProfileRepository,
                                     DoctorAvailabilityRepository doctorAvailabilityRepository,
                                     BlockedSlotRepository blockedSlotRepository,
                                     DoctorScheduleMapper doctorScheduleMapper) {
        this.doctorProfileRepository = doctorProfileRepository;
        this.doctorAvailabilityRepository = doctorAvailabilityRepository;
        this.blockedSlotRepository = blockedSlotRepository;
        this.doctorScheduleMapper = doctorScheduleMapper;
    }

    @Transactional
    public DoctorAvailabilityResponse createAvailability(Long doctorId,
                                                         CreateDoctorAvailabilityRequest request,
                                                         Authentication authentication) {
        DoctorProfile doctorProfile = getDoctorProfileOrThrow(doctorId);
        assertCanManageDoctor(doctorProfile, authentication);
        validateAvailabilityInput(request.dayOfWeek(), request.startTime(), request.endTime(), request.slotDurationMinutes());

        boolean overlaps = doctorAvailabilityRepository
                .existsByDoctorProfileIdAndDayOfWeekAndStartTimeLessThanAndEndTimeGreaterThan(
                        doctorId,
                        request.dayOfWeek(),
                        request.endTime(),
                        request.startTime()
                );

        if (overlaps) {
            throw new DoctorAvailabilityConflictException("Availability overlaps with an existing window");
        }

        DoctorAvailability availability = new DoctorAvailability();
        availability.setDoctorProfile(doctorProfile);
        availability.setDayOfWeek(request.dayOfWeek());
        availability.setStartTime(request.startTime());
        availability.setEndTime(request.endTime());
        availability.setSlotDurationMinutes(request.slotDurationMinutes());

        DoctorAvailability saved = doctorAvailabilityRepository.save(availability);
        return doctorScheduleMapper.toAvailabilityResponse(saved);
    }

    @Transactional
    public DoctorAvailabilityResponse updateAvailability(Long doctorId,
                                                         Long availabilityId,
                                                         UpdateDoctorAvailabilityRequest request,
                                                         Authentication authentication) {
        DoctorProfile doctorProfile = getDoctorProfileOrThrow(doctorId);
        assertCanManageDoctor(doctorProfile, authentication);
        validateAvailabilityInput(request.dayOfWeek(), request.startTime(), request.endTime(), request.slotDurationMinutes());

        DoctorAvailability availability = doctorAvailabilityRepository.findByIdAndDoctorProfileId(availabilityId, doctorId)
                .orElseThrow(() -> new DoctorAvailabilityNotFoundException(availabilityId));

        boolean overlaps = doctorAvailabilityRepository.existsOverlappingExcludingId(
                doctorId,
                request.dayOfWeek(),
                request.startTime(),
                request.endTime(),
                availabilityId
        );

        if (overlaps) {
            throw new DoctorAvailabilityConflictException("Availability overlaps with an existing window");
        }

        availability.setDayOfWeek(request.dayOfWeek());
        availability.setStartTime(request.startTime());
        availability.setEndTime(request.endTime());
        availability.setSlotDurationMinutes(request.slotDurationMinutes());

        DoctorAvailability saved = doctorAvailabilityRepository.save(availability);
        return doctorScheduleMapper.toAvailabilityResponse(saved);
    }

    @Transactional
    public void deleteAvailability(Long doctorId, Long availabilityId, Authentication authentication) {
        DoctorProfile doctorProfile = getDoctorProfileOrThrow(doctorId);
        assertCanManageDoctor(doctorProfile, authentication);

        DoctorAvailability availability = doctorAvailabilityRepository.findByIdAndDoctorProfileId(availabilityId, doctorId)
                .orElseThrow(() -> new DoctorAvailabilityNotFoundException(availabilityId));

        doctorAvailabilityRepository.delete(availability);
    }

    @Transactional
    public BlockedSlotResponse createBlockedSlot(Long doctorId,
                                                 CreateBlockedSlotRequest request,
                                                 Authentication authentication) {
        DoctorProfile doctorProfile = getDoctorProfileOrThrow(doctorId);
        assertCanManageDoctor(doctorProfile, authentication);
        validateBlockedSlotInput(request.blockedDate(), request.startTime(), request.endTime());

        DayOfWeek dayOfWeek = request.blockedDate().getDayOfWeek();
        List<DoctorAvailability> dayAvailabilities = doctorAvailabilityRepository
                .findAllByDoctorProfileIdAndDayOfWeekOrderByStartTimeAsc(doctorId, dayOfWeek);

        boolean fitsAvailability = dayAvailabilities.stream().anyMatch(availability ->
                !request.startTime().isBefore(availability.getStartTime())
                        && !request.endTime().isAfter(availability.getEndTime())
        );

        if (!fitsAvailability) {
            throw new BlockedSlotConflictException("Blocked slot must be inside an existing availability window");
        }

        boolean overlapsBlocked = blockedSlotRepository
                .existsByDoctorProfileIdAndBlockedDateAndStartTimeLessThanAndEndTimeGreaterThan(
                        doctorId,
                        request.blockedDate(),
                        request.endTime(),
                        request.startTime()
                );

        if (overlapsBlocked) {
            throw new BlockedSlotConflictException("Blocked slot overlaps with an existing blocked slot");
        }

        BlockedSlot blockedSlot = new BlockedSlot();
        blockedSlot.setDoctorProfile(doctorProfile);
        blockedSlot.setBlockedDate(request.blockedDate());
        blockedSlot.setStartTime(request.startTime());
        blockedSlot.setEndTime(request.endTime());
        blockedSlot.setReason(request.reason());

        BlockedSlot saved = blockedSlotRepository.save(blockedSlot);
        return doctorScheduleMapper.toBlockedSlotResponse(saved);
    }

    @Transactional(readOnly = true)
    public DoctorScheduleResponse getSchedule(Long doctorId, LocalDate from, LocalDate to) {
        DoctorProfile doctorProfile = getDoctorProfileOrThrow(doctorId);
        validateScheduleRange(from, to);

        List<DoctorAvailability> availabilityRules = doctorAvailabilityRepository
                .findAllByDoctorProfileIdOrderByDayOfWeekAscStartTimeAsc(doctorId);

        List<BlockedSlot> blockedSlots = blockedSlotRepository
                .findAllByDoctorProfileIdAndBlockedDateBetweenOrderByBlockedDateAscStartTimeAsc(doctorId, from, to);

        Map<DayOfWeek, List<DoctorAvailability>> availabilitiesByDay = new HashMap<>();
        for (DoctorAvailability availability : availabilityRules) {
            availabilitiesByDay.computeIfAbsent(availability.getDayOfWeek(), key -> new ArrayList<>()).add(availability);
        }

        Map<LocalDate, List<BlockedSlot>> blockedByDate = new HashMap<>();
        for (BlockedSlot blockedSlot : blockedSlots) {
            blockedByDate.computeIfAbsent(blockedSlot.getBlockedDate(), key -> new ArrayList<>()).add(blockedSlot);
        }

        List<DoctorScheduleResponse.DoctorScheduleDayResponse> days = new ArrayList<>();
        LocalDate dateCursor = from;
        while (!dateCursor.isAfter(to)) {
            DayOfWeek dayOfWeek = dateCursor.getDayOfWeek();

            List<DoctorScheduleResponse.DoctorScheduleWindowResponse> windows =
                    availabilitiesByDay.getOrDefault(dayOfWeek, List.of()).stream()
                            .map(availability -> new DoctorScheduleResponse.DoctorScheduleWindowResponse(
                                    availability.getId(),
                                    availability.getStartTime(),
                                    availability.getEndTime(),
                                    availability.getSlotDurationMinutes()
                            ))
                            .toList();

            List<DoctorScheduleResponse.DoctorScheduleBlockedSlotResponse> blocked =
                    blockedByDate.getOrDefault(dateCursor, List.of()).stream()
                            .map(slot -> new DoctorScheduleResponse.DoctorScheduleBlockedSlotResponse(
                                    slot.getId(),
                                    slot.getStartTime(),
                                    slot.getEndTime()
                            ))
                            .toList();

            days.add(new DoctorScheduleResponse.DoctorScheduleDayResponse(
                    dateCursor,
                    dayOfWeek,
                    windows,
                    blocked
            ));

            dateCursor = dateCursor.plusDays(1);
        }

        return new DoctorScheduleResponse(
                doctorProfile.getId(),
                from,
                to,
                availabilityRules.stream().map(doctorScheduleMapper::toAvailabilityResponse).toList(),
                days
        );
    }

    private DoctorProfile getDoctorProfileOrThrow(Long doctorId) {
        return doctorProfileRepository.findByIdAndActiveTrue(doctorId)
                .orElseThrow(() -> new DoctorProfileNotFoundException(doctorId));
    }

    private void assertCanManageDoctor(DoctorProfile doctorProfile, Authentication authentication) {
        if (authentication == null || authentication.getAuthorities() == null) {
            throw new AccessDeniedException("Authentication is required");
        }

        if (hasRole(authentication, "ADMIN")) {
            return;
        }

        if (!hasRole(authentication, "DOCTOR")) {
            throw new AccessDeniedException("You are not allowed to manage doctor availability");
        }

        String ownerEmail = normalizeEmail(authentication.getName());
        if (!ownerEmail.equalsIgnoreCase(doctorProfile.getOwnerEmail())) {
            throw new AccessDeniedException("You are not allowed to manage another doctor's availability");
        }
    }

    private boolean hasRole(Authentication authentication, String role) {
        String expectedAuthority = "ROLE_" + role;
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> expectedAuthority.equals(authority.getAuthority()));
    }

    private void validateAvailabilityInput(DayOfWeek dayOfWeek,
                                           LocalTime startTime,
                                           LocalTime endTime,
                                           Integer slotDurationMinutes) {
        if (dayOfWeek == null || startTime == null || endTime == null || slotDurationMinutes == null) {
            throw new IllegalArgumentException("Availability payload is incomplete");
        }

        if (!startTime.isBefore(endTime)) {
            throw new DoctorAvailabilityConflictException("Availability startTime must be before endTime");
        }

        int totalMinutes = (endTime.getHour() * 60 + endTime.getMinute())
                - (startTime.getHour() * 60 + startTime.getMinute());

        if (totalMinutes % slotDurationMinutes != 0) {
            throw new DoctorAvailabilityConflictException("Availability window must be divisible by slotDurationMinutes");
        }
    }

    private void validateBlockedSlotInput(LocalDate blockedDate, LocalTime startTime, LocalTime endTime) {
        if (blockedDate == null || startTime == null || endTime == null) {
            throw new IllegalArgumentException("Blocked slot payload is incomplete");
        }

        if (!startTime.isBefore(endTime)) {
            throw new BlockedSlotConflictException("Blocked slot startTime must be before endTime");
        }
    }

    private void validateScheduleRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("from and to are required");
        }

        if (from.isAfter(to)) {
            throw new IllegalArgumentException("from must be before or equal to to");
        }

        if (from.plusDays(MAX_SCHEDULE_RANGE_DAYS - 1L).isBefore(to)) {
            throw new IllegalArgumentException("Date range cannot exceed " + MAX_SCHEDULE_RANGE_DAYS + " days");
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
