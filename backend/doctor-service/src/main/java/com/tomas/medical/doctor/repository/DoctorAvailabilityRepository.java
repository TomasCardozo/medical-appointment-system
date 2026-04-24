package com.tomas.medical.doctor.repository;

import com.tomas.medical.doctor.entity.DoctorAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface DoctorAvailabilityRepository extends JpaRepository<DoctorAvailability, Long> {

    Optional<DoctorAvailability> findByIdAndDoctorProfileId(Long id, Long doctorProfileId);

    List<DoctorAvailability> findAllByDoctorProfileIdOrderByDayOfWeekAscStartTimeAsc(Long doctorProfileId);

    List<DoctorAvailability> findAllByDoctorProfileIdAndDayOfWeekOrderByStartTimeAsc(Long doctorProfileId, DayOfWeek dayOfWeek);

    boolean existsByDoctorProfileIdAndDayOfWeekAndStartTimeLessThanAndEndTimeGreaterThan(
            Long doctorProfileId,
            DayOfWeek dayOfWeek,
            LocalTime endTime,
            LocalTime startTime
    );

    @Query("""
            SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END
            FROM DoctorAvailability a
            WHERE a.doctorProfile.id = :doctorProfileId
              AND a.dayOfWeek = :dayOfWeek
              AND a.startTime < :endTime
              AND a.endTime > :startTime
              AND a.id <> :excludeId
            """)
    boolean existsOverlappingExcludingId(@Param("doctorProfileId") Long doctorProfileId,
                                         @Param("dayOfWeek") DayOfWeek dayOfWeek,
                                         @Param("startTime") LocalTime startTime,
                                         @Param("endTime") LocalTime endTime,
                                         @Param("excludeId") Long excludeId);
}
