package com.tomas.medical.doctor.controller;

import com.tomas.medical.doctor.dto.response.InternalDoctorOwnerResponse;
import com.tomas.medical.doctor.dto.response.InternalDoctorSummaryResponse;
import com.tomas.medical.doctor.service.DoctorProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/doctors")
public class InternalDoctorController {

    private final DoctorProfileService doctorProfileService;

    public InternalDoctorController(DoctorProfileService doctorProfileService) {
        this.doctorProfileService = doctorProfileService;
    }

    @GetMapping("/by-owner-email")
    public ResponseEntity<InternalDoctorOwnerResponse> getDoctorByOwnerEmail(@RequestParam String email) {
        return ResponseEntity.ok(doctorProfileService.getDoctorByOwnerEmail(email));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InternalDoctorSummaryResponse> getDoctorById(@PathVariable Long id) {
        return ResponseEntity.ok(doctorProfileService.getDoctorSummaryById(id));
    }
}
