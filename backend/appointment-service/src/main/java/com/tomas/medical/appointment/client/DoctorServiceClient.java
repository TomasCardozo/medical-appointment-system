package com.tomas.medical.appointment.client;

import com.tomas.medical.appointment.client.dto.DoctorScheduleResponse;
import com.tomas.medical.appointment.client.dto.InternalDoctorOwnerResponse;
import com.tomas.medical.appointment.client.dto.InternalDoctorSummaryResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@FeignClient(name = "doctor-service")
public interface DoctorServiceClient {

    @GetMapping("/doctors/{id}/schedule")
    DoctorScheduleResponse getDoctorSchedule(@PathVariable("id") Long doctorId,
                                             @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                             @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to);

    @GetMapping("/internal/doctors/by-owner-email")
    InternalDoctorOwnerResponse getDoctorByOwnerEmail(@RequestParam("email") String email);

    @GetMapping("/internal/doctors/{id}")
    InternalDoctorSummaryResponse getDoctorById(@PathVariable("id") Long id);
}
