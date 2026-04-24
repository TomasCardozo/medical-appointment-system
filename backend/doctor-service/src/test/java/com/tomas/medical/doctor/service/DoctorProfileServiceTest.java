package com.tomas.medical.doctor.service;

import com.tomas.medical.doctor.dto.request.CreateDoctorProfileRequest;
import com.tomas.medical.doctor.dto.request.UpdateDoctorProfileRequest;
import com.tomas.medical.doctor.dto.response.DoctorProfileResponse;
import com.tomas.medical.doctor.dto.response.InternalDoctorSummaryResponse;
import com.tomas.medical.doctor.entity.DoctorProfile;
import com.tomas.medical.doctor.exception.DoctorProfileAlreadyExistsException;
import com.tomas.medical.doctor.exception.DoctorProfileNotFoundException;
import com.tomas.medical.doctor.exception.DoctorProfileOwnerNotFoundException;
import com.tomas.medical.doctor.mapper.DoctorProfileMapper;
import com.tomas.medical.doctor.repository.DoctorProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DoctorProfileServiceTest {

    @Mock
    private DoctorProfileRepository doctorProfileRepository;

    private DoctorProfileService doctorProfileService;

    @BeforeEach
    void setUp() {
        doctorProfileService = new DoctorProfileService(doctorProfileRepository, new DoctorProfileMapper());
    }

    @Test
    void createProfileThrowsWhenOwnerAlreadyHasProfile() {
        var auth = new UsernamePasswordAuthenticationToken(
                "doctor@example.com",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_DOCTOR"))
        );

        when(doctorProfileRepository.existsByOwnerEmailIgnoreCase("doctor@example.com")).thenReturn(true);

        assertThrows(
                DoctorProfileAlreadyExistsException.class,
                () -> doctorProfileService.createProfile(validRequest(), auth)
        );
    }

    @Test
    void getProfileByIdThrowsWhenNotFound() {
        when(doctorProfileRepository.findByIdAndActiveTrue(10L)).thenReturn(Optional.empty());

        assertThrows(
                DoctorProfileNotFoundException.class,
                () -> doctorProfileService.getProfileById(10L)
        );
    }

    @Test
    void getMyProfileReturnsAuthenticatedDoctorProfile() {
        var auth = new UsernamePasswordAuthenticationToken(
                "Doctor@Example.com",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_DOCTOR"))
        );

        DoctorProfile profile = new DoctorProfile();
        profile.setId(1L);
        profile.setOwnerEmail("doctor@example.com");
        profile.setFullName("Dra. Laura Diaz");
        profile.setSpecialty("Cardiology");
        profile.setLicenseNumber("MAT-100");
        profile.setClinicAddress("Main St 123");
        profile.setBio("Specialist in clinical cardiology");
        profile.setActive(true);

        when(doctorProfileRepository.findByOwnerEmailIgnoreCaseAndActiveTrue("doctor@example.com"))
                .thenReturn(Optional.of(profile));

        DoctorProfileResponse result = doctorProfileService.getMyProfile(auth);

        assertEquals(1L, result.id());
        assertEquals("Dra. Laura Diaz", result.fullName());
        assertEquals("Cardiology", result.specialty());
    }

    @Test
    void getMyProfileThrowsWhenAuthenticatedUserHasNoDoctorProfile() {
        var auth = new UsernamePasswordAuthenticationToken(
                "patient@example.com",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_PATIENT"))
        );

        when(doctorProfileRepository.findByOwnerEmailIgnoreCaseAndActiveTrue("patient@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                DoctorProfileOwnerNotFoundException.class,
                () -> doctorProfileService.getMyProfile(auth)
        );
    }

    @Test
    void listProfilesReturnsMappedProfiles() {
        DoctorProfile profile = new DoctorProfile();
        profile.setId(1L);
        profile.setFullName("Dra. Laura Diaz");
        profile.setSpecialty("Cardiology");
        profile.setLicenseNumber("MAT-100");
        profile.setClinicAddress("Main St 123");
        profile.setBio("Specialist in clinical cardiology");
        profile.setActive(true);

        when(doctorProfileRepository.findAllByActiveTrueOrderByFullNameAsc()).thenReturn(List.of(profile));

        List<DoctorProfileResponse> result = doctorProfileService.listProfiles();

        assertEquals(1, result.size());
        assertEquals("Dra. Laura Diaz", result.getFirst().fullName());
        assertEquals("Cardiology", result.getFirst().specialty());
    }

    @Test
    void updateMyProfileUpdatesAuthenticatedDoctorData() {
        var auth = new UsernamePasswordAuthenticationToken(
                "Doctor@Example.com",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_DOCTOR"))
        );

        DoctorProfile profile = new DoctorProfile();
        profile.setId(1L);
        profile.setOwnerEmail("doctor@example.com");
        profile.setFullName("Dra. Laura Diaz");
        profile.setSpecialty("Cardiology");
        profile.setLicenseNumber("MAT-100");
        profile.setClinicAddress("Main St 123");
        profile.setBio("Specialist in clinical cardiology");
        profile.setActive(true);

        when(doctorProfileRepository.findByOwnerEmailIgnoreCaseAndActiveTrue("doctor@example.com"))
                .thenReturn(Optional.of(profile));
        when(doctorProfileRepository.save(any(DoctorProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DoctorProfileResponse result = doctorProfileService.updateMyProfile(
                new UpdateDoctorProfileRequest(
                        "Dra. Laura Updated",
                        "Internal Medicine",
                        "MAT-101",
                        "Main St 456",
                        "Updated bio"
                ),
                auth
        );

        assertEquals("Dra. Laura Updated", result.fullName());
        assertEquals("Internal Medicine", result.specialty());
        assertEquals("MAT-101", result.licenseNumber());
        assertEquals("Main St 456", result.clinicAddress());
        assertEquals("Updated bio", result.bio());
    }

    @Test
    void getDoctorSummaryByIdReturnsActiveDoctorData() {
        DoctorProfile profile = new DoctorProfile();
        profile.setId(1L);
        profile.setFullName("Dra. Laura Diaz");
        profile.setActive(true);

        when(doctorProfileRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(profile));

        InternalDoctorSummaryResponse result = doctorProfileService.getDoctorSummaryById(1L);

        assertEquals(1L, result.id());
        assertEquals("Dra. Laura Diaz", result.fullName());
        assertEquals(true, result.active());
    }

    private CreateDoctorProfileRequest validRequest() {
        return new CreateDoctorProfileRequest(
                "Dra. Laura Diaz",
                "Cardiology",
                "MAT-100",
                "Main St 123",
                "Specialist in clinical cardiology"
        );
    }
}
