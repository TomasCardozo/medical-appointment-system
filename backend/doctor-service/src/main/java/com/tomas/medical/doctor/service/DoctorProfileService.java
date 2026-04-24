package com.tomas.medical.doctor.service;

import com.tomas.medical.doctor.dto.request.CreateDoctorProfileRequest;
import com.tomas.medical.doctor.dto.request.UpdateDoctorProfileRequest;
import com.tomas.medical.doctor.dto.response.DoctorProfileResponse;
import com.tomas.medical.doctor.dto.response.InternalDoctorOwnerResponse;
import com.tomas.medical.doctor.dto.response.InternalDoctorSummaryResponse;
import com.tomas.medical.doctor.entity.DoctorProfile;
import com.tomas.medical.doctor.exception.DoctorProfileAlreadyExistsException;
import com.tomas.medical.doctor.exception.DoctorProfileNotFoundException;
import com.tomas.medical.doctor.exception.DoctorProfileOwnerNotFoundException;
import com.tomas.medical.doctor.mapper.DoctorProfileMapper;
import com.tomas.medical.doctor.repository.DoctorProfileRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DoctorProfileService {

    private final DoctorProfileRepository doctorProfileRepository;
    private final DoctorProfileMapper doctorProfileMapper;

    public DoctorProfileService(DoctorProfileRepository doctorProfileRepository,
                                DoctorProfileMapper doctorProfileMapper) {
        this.doctorProfileRepository = doctorProfileRepository;
        this.doctorProfileMapper = doctorProfileMapper;
    }

    @Transactional
    public DoctorProfileResponse createProfile(CreateDoctorProfileRequest request, Authentication authentication) {
        if (!hasAnyRole(authentication, "DOCTOR")) {
            throw new AccessDeniedException("You are not allowed to create doctor profiles");
        }

        String ownerEmail = normalizeEmail(authentication.getName());

        if (doctorProfileRepository.existsByOwnerEmailIgnoreCase(ownerEmail)) {
            throw new DoctorProfileAlreadyExistsException(ownerEmail);
        }

        DoctorProfile profile = doctorProfileMapper.toEntity(request, ownerEmail);
        DoctorProfile saved = doctorProfileRepository.save(profile);
        return doctorProfileMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<DoctorProfileResponse> listProfiles() {
        return doctorProfileRepository.findAllByActiveTrueOrderByFullNameAsc().stream()
                .map(doctorProfileMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DoctorProfileResponse getMyProfile(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new AccessDeniedException("Authentication is required");
        }

        DoctorProfile profile = findActiveProfileByOwnerEmail(authentication.getName());
        return doctorProfileMapper.toResponse(profile);
    }

    @Transactional
    public DoctorProfileResponse updateMyProfile(UpdateDoctorProfileRequest request, Authentication authentication) {
        if (!hasAnyRole(authentication, "DOCTOR")) {
            throw new AccessDeniedException("You are not allowed to update doctor profiles");
        }

        DoctorProfile profile = findActiveProfileByOwnerEmail(authentication.getName());
        profile.setFullName(request.fullName());
        profile.setSpecialty(request.specialty());
        profile.setLicenseNumber(request.licenseNumber());
        profile.setClinicAddress(request.clinicAddress());
        profile.setBio(request.bio());

        DoctorProfile saved = doctorProfileRepository.save(profile);
        return doctorProfileMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public DoctorProfileResponse getProfileById(Long doctorId) {
        DoctorProfile profile = doctorProfileRepository.findByIdAndActiveTrue(doctorId)
                .orElseThrow(() -> new DoctorProfileNotFoundException(doctorId));
        return doctorProfileMapper.toResponse(profile);
    }

    @Transactional(readOnly = true)
    public InternalDoctorOwnerResponse getDoctorByOwnerEmail(String ownerEmail) {
        DoctorProfile profile = findActiveProfileByOwnerEmail(ownerEmail);

        return new InternalDoctorOwnerResponse(
                profile.getId(),
                profile.getOwnerEmail(),
                profile.isActive()
        );
    }

    @Transactional(readOnly = true)
    public InternalDoctorSummaryResponse getDoctorSummaryById(Long doctorId) {
        DoctorProfile profile = doctorProfileRepository.findByIdAndActiveTrue(doctorId)
                .orElseThrow(() -> new DoctorProfileNotFoundException(doctorId));

        return new InternalDoctorSummaryResponse(
                profile.getId(),
                profile.getFullName(),
                profile.isActive()
        );
    }

    private DoctorProfile findActiveProfileByOwnerEmail(String ownerEmail) {
        String normalizedEmail = normalizeEmail(ownerEmail);
        return doctorProfileRepository.findByOwnerEmailIgnoreCaseAndActiveTrue(normalizedEmail)
                .orElseThrow(() -> new DoctorProfileOwnerNotFoundException(normalizedEmail));
    }

    private boolean hasAnyRole(Authentication authentication, String... allowedRoles) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }

        for (String role : allowedRoles) {
            String expectedAuthority = "ROLE_" + role;
            boolean matches = authentication.getAuthorities().stream()
                    .anyMatch(authority -> expectedAuthority.equals(authority.getAuthority()));

            if (matches) {
                return true;
            }
        }

        return false;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
