package com.tomas.medical.appointment.controller;

import com.tomas.medical.appointment.dto.request.CancelAppointmentRequest;
import com.tomas.medical.appointment.dto.request.CreateAppointmentRequest;
import com.tomas.medical.appointment.dto.request.RescheduleAppointmentRequest;
import com.tomas.medical.appointment.dto.response.AppointmentResponse;
import com.tomas.medical.appointment.dto.response.AvailableSlotResponse;
import com.tomas.medical.appointment.service.AppointmentService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping("/available")
    public ResponseEntity<List<AvailableSlotResponse>> getAvailableSlots(@RequestParam Long doctorId,
                                                                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(appointmentService.getAvailableSlots(doctorId, date));
    }

    @PostMapping
    public ResponseEntity<AppointmentResponse> createAppointment(@Valid @RequestBody CreateAppointmentRequest request,
                                                                 Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(appointmentService.createAppointment(request, authentication));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<AppointmentResponse> cancelAppointment(@PathVariable Long id,
                                                                 @Valid @RequestBody CancelAppointmentRequest request,
                                                                 Authentication authentication) {
        return ResponseEntity.ok(appointmentService.cancelAppointment(id, request, authentication));
    }

    @PutMapping("/{id}/reschedule")
    public ResponseEntity<AppointmentResponse> rescheduleAppointment(@PathVariable Long id,
                                                                     @Valid @RequestBody RescheduleAppointmentRequest request,
                                                                     Authentication authentication) {
        return ResponseEntity.ok(appointmentService.rescheduleAppointment(id, request, authentication));
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<AppointmentResponse>> getPatientAppointments(@PathVariable Long patientId,
                                                                            Authentication authentication) {
        return ResponseEntity.ok(appointmentService.getPatientAppointments(patientId, authentication));
    }

    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<List<AppointmentResponse>> getDoctorAppointments(@PathVariable Long doctorId,
                                                                           Authentication authentication) {
        return ResponseEntity.ok(appointmentService.getDoctorAppointments(doctorId, authentication));
    }

    @GetMapping("/doctor/{doctorId}/agenda")
    public ResponseEntity<List<AppointmentResponse>> getDoctorAgenda(@PathVariable Long doctorId,
                                                                     @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                                     @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                                                     Authentication authentication) {
        return ResponseEntity.ok(appointmentService.getDoctorAgenda(doctorId, from, to, authentication));
    }
}
