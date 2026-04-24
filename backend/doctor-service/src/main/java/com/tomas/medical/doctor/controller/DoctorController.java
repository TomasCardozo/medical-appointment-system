package com.tomas.medical.doctor.controller;

import com.tomas.medical.doctor.dto.request.CreateDoctorProfileRequest;
import com.tomas.medical.doctor.dto.request.CreateBlockedSlotRequest;
import com.tomas.medical.doctor.dto.request.CreateDoctorAvailabilityRequest;
import com.tomas.medical.doctor.dto.request.UpdateDoctorProfileRequest;
import com.tomas.medical.doctor.dto.request.UpdateDoctorAvailabilityRequest;
import com.tomas.medical.doctor.dto.response.BlockedSlotResponse;
import com.tomas.medical.doctor.dto.response.DoctorAvailabilityResponse;
import com.tomas.medical.doctor.dto.response.DoctorProfileResponse;
import com.tomas.medical.doctor.dto.response.DoctorScheduleResponse;
import com.tomas.medical.doctor.service.DoctorAvailabilityService;
import com.tomas.medical.doctor.service.DoctorProfileService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/doctors")
public class DoctorController {

    private final DoctorProfileService doctorProfileService;
    private final DoctorAvailabilityService doctorAvailabilityService;

    public DoctorController(DoctorProfileService doctorProfileService,
                            DoctorAvailabilityService doctorAvailabilityService) {
        this.doctorProfileService = doctorProfileService;
        this.doctorAvailabilityService = doctorAvailabilityService;
    }

    @PostMapping("/profile")
    public ResponseEntity<DoctorProfileResponse> createProfile(@Valid @RequestBody CreateDoctorProfileRequest request,
                                                               Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(doctorProfileService.createProfile(request, authentication));
    }

    @GetMapping("/me")
    public ResponseEntity<DoctorProfileResponse> getMyDoctor(Authentication authentication) {
        return ResponseEntity.ok(doctorProfileService.getMyProfile(authentication));
    }

    @PutMapping("/me")
    public ResponseEntity<DoctorProfileResponse> updateMyDoctor(@Valid @RequestBody UpdateDoctorProfileRequest request,
                                                                Authentication authentication) {
        return ResponseEntity.ok(doctorProfileService.updateMyProfile(request, authentication));
    }

    @GetMapping
    public ResponseEntity<List<DoctorProfileResponse>> listDoctors() {
        return ResponseEntity.ok(doctorProfileService.listProfiles());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DoctorProfileResponse> getDoctorById(@PathVariable Long id) {
        return ResponseEntity.ok(doctorProfileService.getProfileById(id));
    }

    @PostMapping("/{id}/availability")
    public ResponseEntity<DoctorAvailabilityResponse> createAvailability(@PathVariable Long id,
                                                                         @Valid @RequestBody CreateDoctorAvailabilityRequest request,
                                                                         Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(doctorAvailabilityService.createAvailability(id, request, authentication));
    }

    @PutMapping("/{id}/availability/{availabilityId}")
    public ResponseEntity<DoctorAvailabilityResponse> updateAvailability(@PathVariable Long id,
                                                                         @PathVariable Long availabilityId,
                                                                         @Valid @RequestBody UpdateDoctorAvailabilityRequest request,
                                                                         Authentication authentication) {
        return ResponseEntity.ok(doctorAvailabilityService.updateAvailability(id, availabilityId, request, authentication));
    }

    @DeleteMapping("/{id}/availability/{availabilityId}")
    public ResponseEntity<Void> deleteAvailability(@PathVariable Long id,
                                                   @PathVariable Long availabilityId,
                                                   Authentication authentication) {
        doctorAvailabilityService.deleteAvailability(id, availabilityId, authentication);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/blocked-slots")
    public ResponseEntity<BlockedSlotResponse> createBlockedSlot(@PathVariable Long id,
                                                                 @Valid @RequestBody CreateBlockedSlotRequest request,
                                                                 Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(doctorAvailabilityService.createBlockedSlot(id, request, authentication));
    }

    @GetMapping("/{id}/schedule")
    public ResponseEntity<DoctorScheduleResponse> getSchedule(@PathVariable Long id,
                                                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(doctorAvailabilityService.getSchedule(id, from, to));
    }
}
