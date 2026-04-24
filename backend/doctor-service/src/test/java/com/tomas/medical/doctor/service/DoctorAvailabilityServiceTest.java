package com.tomas.medical.doctor.service;

import com.tomas.medical.doctor.dto.request.CreateBlockedSlotRequest;
import com.tomas.medical.doctor.dto.request.CreateDoctorAvailabilityRequest;
import com.tomas.medical.doctor.dto.request.UpdateDoctorAvailabilityRequest;
import com.tomas.medical.doctor.dto.response.DoctorScheduleResponse;
import com.tomas.medical.doctor.entity.BlockedSlot;
import com.tomas.medical.doctor.entity.DoctorAvailability;
import com.tomas.medical.doctor.entity.DoctorProfile;
import com.tomas.medical.doctor.exception.BlockedSlotConflictException;
import com.tomas.medical.doctor.exception.DoctorAvailabilityConflictException;
import com.tomas.medical.doctor.exception.DoctorAvailabilityNotFoundException;
import com.tomas.medical.doctor.mapper.DoctorScheduleMapper;
import com.tomas.medical.doctor.repository.BlockedSlotRepository;
import com.tomas.medical.doctor.repository.DoctorAvailabilityRepository;
import com.tomas.medical.doctor.repository.DoctorProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DoctorAvailabilityServiceTest {

    @Mock
    private DoctorProfileRepository doctorProfileRepository;

    @Mock
    private DoctorAvailabilityRepository doctorAvailabilityRepository;

    @Mock
    private BlockedSlotRepository blockedSlotRepository;

    private DoctorAvailabilityService doctorAvailabilityService;

    @BeforeEach
    void setUp() {
        doctorAvailabilityService = new DoctorAvailabilityService(
                doctorProfileRepository,
                doctorAvailabilityRepository,
                blockedSlotRepository,
                new DoctorScheduleMapper()
        );
    }

    @Test
    void createAvailabilityRejectsWhenDoctorTriesToManageAnotherProfile() {
        DoctorProfile profile = activeDoctor(10L, "owner@example.com");
        when(doctorProfileRepository.findByIdAndActiveTrue(10L)).thenReturn(Optional.of(profile));

        var auth = doctorAuth("another@example.com");

        assertThrows(
                AccessDeniedException.class,
                () -> doctorAvailabilityService.createAvailability(10L, validCreateAvailability(), auth)
        );
    }

    @Test
    void createAvailabilityRejectsOverlappingWindows() {
        DoctorProfile profile = activeDoctor(10L, "owner@example.com");
        when(doctorProfileRepository.findByIdAndActiveTrue(10L)).thenReturn(Optional.of(profile));
        when(doctorAvailabilityRepository.existsByDoctorProfileIdAndDayOfWeekAndStartTimeLessThanAndEndTimeGreaterThan(
                10L,
                DayOfWeek.MONDAY,
                LocalTime.of(12, 0),
                LocalTime.of(9, 0)
        )).thenReturn(true);

        assertThrows(
                DoctorAvailabilityConflictException.class,
                () -> doctorAvailabilityService.createAvailability(10L, validCreateAvailability(), doctorAuth("owner@example.com"))
        );
    }

    @Test
    void updateAvailabilityRejectsWhenAvailabilityNotFound() {
        DoctorProfile profile = activeDoctor(10L, "owner@example.com");
        when(doctorProfileRepository.findByIdAndActiveTrue(10L)).thenReturn(Optional.of(profile));
        when(doctorAvailabilityRepository.findByIdAndDoctorProfileId(77L, 10L)).thenReturn(Optional.empty());

        assertThrows(
                DoctorAvailabilityNotFoundException.class,
                () -> doctorAvailabilityService.updateAvailability(
                        10L,
                        77L,
                        validUpdateAvailability(),
                        doctorAuth("owner@example.com")
                )
        );
    }

    @Test
    void deleteAvailabilityRemovesAvailabilityWhenOwnerIsDoctor() {
        DoctorProfile profile = activeDoctor(10L, "owner@example.com");
        DoctorAvailability availability = new DoctorAvailability();
        availability.setId(20L);
        availability.setDoctorProfile(profile);

        when(doctorProfileRepository.findByIdAndActiveTrue(10L)).thenReturn(Optional.of(profile));
        when(doctorAvailabilityRepository.findByIdAndDoctorProfileId(20L, 10L)).thenReturn(Optional.of(availability));

        doctorAvailabilityService.deleteAvailability(10L, 20L, doctorAuth("owner@example.com"));

        verify(doctorAvailabilityRepository).delete(availability);
    }

    @Test
    void createBlockedSlotRejectsWhenOutsideAvailability() {
        DoctorProfile profile = activeDoctor(10L, "owner@example.com");
        DoctorAvailability availability = new DoctorAvailability();
        availability.setStartTime(LocalTime.of(9, 0));
        availability.setEndTime(LocalTime.of(10, 0));
        availability.setDayOfWeek(DayOfWeek.MONDAY);

        when(doctorProfileRepository.findByIdAndActiveTrue(10L)).thenReturn(Optional.of(profile));
        when(doctorAvailabilityRepository.findAllByDoctorProfileIdAndDayOfWeekOrderByStartTimeAsc(10L, DayOfWeek.MONDAY))
                .thenReturn(List.of(availability));

        CreateBlockedSlotRequest blockedSlotRequest = new CreateBlockedSlotRequest(
                LocalDate.of(2026, 4, 20),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                null
        );

        assertThrows(
                BlockedSlotConflictException.class,
                () -> doctorAvailabilityService.createBlockedSlot(10L, blockedSlotRequest, doctorAuth("owner@example.com"))
        );

        verify(blockedSlotRepository, never()).save(any());
    }

    @Test
    void getScheduleReturnsWindowAndBlockedSlotByDate() {
        DoctorProfile profile = activeDoctor(10L, "owner@example.com");

        DoctorAvailability availability = new DoctorAvailability();
        availability.setId(1L);
        availability.setDoctorProfile(profile);
        availability.setDayOfWeek(DayOfWeek.MONDAY);
        availability.setStartTime(LocalTime.of(9, 0));
        availability.setEndTime(LocalTime.of(12, 0));
        availability.setSlotDurationMinutes(30);

        BlockedSlot blockedSlot = new BlockedSlot();
        blockedSlot.setId(2L);
        blockedSlot.setDoctorProfile(profile);
        blockedSlot.setBlockedDate(LocalDate.of(2026, 4, 20));
        blockedSlot.setStartTime(LocalTime.of(10, 0));
        blockedSlot.setEndTime(LocalTime.of(10, 30));

        when(doctorProfileRepository.findByIdAndActiveTrue(10L)).thenReturn(Optional.of(profile));
        when(doctorAvailabilityRepository.findAllByDoctorProfileIdOrderByDayOfWeekAscStartTimeAsc(10L))
                .thenReturn(List.of(availability));
        when(blockedSlotRepository.findAllByDoctorProfileIdAndBlockedDateBetweenOrderByBlockedDateAscStartTimeAsc(
                10L,
                LocalDate.of(2026, 4, 20),
                LocalDate.of(2026, 4, 20)
        )).thenReturn(List.of(blockedSlot));

        DoctorScheduleResponse response = doctorAvailabilityService.getSchedule(
                10L,
                LocalDate.of(2026, 4, 20),
                LocalDate.of(2026, 4, 20)
        );

        assertEquals(10L, response.doctorId());
        assertEquals(1, response.availabilityRules().size());
        assertEquals(1, response.days().size());
        assertEquals(1, response.days().getFirst().availableWindows().size());
        assertEquals(1, response.days().getFirst().blockedSlots().size());
    }

    private DoctorProfile activeDoctor(Long id, String ownerEmail) {
        DoctorProfile doctorProfile = new DoctorProfile();
        doctorProfile.setId(id);
        doctorProfile.setOwnerEmail(ownerEmail);
        doctorProfile.setActive(true);
        return doctorProfile;
    }

    private CreateDoctorAvailabilityRequest validCreateAvailability() {
        return new CreateDoctorAvailabilityRequest(
                DayOfWeek.MONDAY,
                LocalTime.of(9, 0),
                LocalTime.of(12, 0),
                30
        );
    }

    private UpdateDoctorAvailabilityRequest validUpdateAvailability() {
        return new UpdateDoctorAvailabilityRequest(
                DayOfWeek.MONDAY,
                LocalTime.of(13, 0),
                LocalTime.of(16, 0),
                30
        );
    }

    private UsernamePasswordAuthenticationToken doctorAuth(String email) {
        return new UsernamePasswordAuthenticationToken(
                email,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_DOCTOR"))
        );
    }
}
