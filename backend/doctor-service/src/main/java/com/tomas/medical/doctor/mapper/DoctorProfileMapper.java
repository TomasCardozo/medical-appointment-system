package com.tomas.medical.doctor.mapper;

import com.tomas.medical.doctor.dto.request.CreateDoctorProfileRequest;
import com.tomas.medical.doctor.dto.response.DoctorProfileResponse;
import com.tomas.medical.doctor.entity.DoctorProfile;
import org.springframework.stereotype.Component;

@Component
public class DoctorProfileMapper {

    public DoctorProfile toEntity(CreateDoctorProfileRequest request, String ownerEmail) {
        DoctorProfile profile = new DoctorProfile();
        profile.setOwnerEmail(ownerEmail);
        profile.setFullName(request.fullName());
        profile.setSpecialty(request.specialty());
        profile.setLicenseNumber(request.licenseNumber());
        profile.setClinicAddress(request.clinicAddress());
        profile.setBio(request.bio());
        profile.setActive(true);
        return profile;
    }

    public DoctorProfileResponse toResponse(DoctorProfile profile) {
        return new DoctorProfileResponse(
                profile.getId(),
                profile.getFullName(),
                profile.getSpecialty(),
                profile.getLicenseNumber(),
                profile.getClinicAddress(),
                profile.getBio()
        );
    }
}
