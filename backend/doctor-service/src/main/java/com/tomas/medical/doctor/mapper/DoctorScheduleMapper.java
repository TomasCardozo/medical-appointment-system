package com.tomas.medical.doctor.mapper;

import com.tomas.medical.doctor.dto.response.BlockedSlotResponse;
import com.tomas.medical.doctor.dto.response.DoctorAvailabilityResponse;
import com.tomas.medical.doctor.entity.BlockedSlot;
import com.tomas.medical.doctor.entity.DoctorAvailability;
import org.springframework.stereotype.Component;

@Component
public class DoctorScheduleMapper {

    public DoctorAvailabilityResponse toAvailabilityResponse(DoctorAvailability availability) {
        return new DoctorAvailabilityResponse(
                availability.getId(),
                availability.getDayOfWeek(),
                availability.getStartTime(),
                availability.getEndTime(),
                availability.getSlotDurationMinutes()
        );
    }

    public BlockedSlotResponse toBlockedSlotResponse(BlockedSlot blockedSlot) {
        return new BlockedSlotResponse(
                blockedSlot.getId(),
                blockedSlot.getBlockedDate(),
                blockedSlot.getStartTime(),
                blockedSlot.getEndTime()
        );
    }
}
