package com.tomas.medical.doctor.repository;

import com.tomas.medical.doctor.entity.BlockedSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface BlockedSlotRepository extends JpaRepository<BlockedSlot, Long> {

    boolean existsByDoctorProfileIdAndBlockedDateAndStartTimeLessThanAndEndTimeGreaterThan(
            Long doctorProfileId,
            LocalDate blockedDate,
            LocalTime endTime,
            LocalTime startTime
    );

    List<BlockedSlot> findAllByDoctorProfileIdAndBlockedDateBetweenOrderByBlockedDateAscStartTimeAsc(
            Long doctorProfileId,
            LocalDate from,
            LocalDate to
    );
}
