package com.tomas.medical.doctor.repository;

import com.tomas.medical.doctor.entity.DoctorProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DoctorProfileRepository extends JpaRepository<DoctorProfile, Long> {

    boolean existsByOwnerEmailIgnoreCase(String ownerEmail);

    List<DoctorProfile> findAllByActiveTrueOrderByFullNameAsc();

    Optional<DoctorProfile> findByIdAndActiveTrue(Long id);

    Optional<DoctorProfile> findByOwnerEmailIgnoreCaseAndActiveTrue(String ownerEmail);
}
