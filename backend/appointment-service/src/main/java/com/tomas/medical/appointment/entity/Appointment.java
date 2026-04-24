package com.tomas.medical.appointment.entity;

import com.tomas.medical.appointment.enumtype.AppointmentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

@Entity
@Table(name = "appointments")
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "doctor_id", nullable = false)
    private Long doctorId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "appointment_date", nullable = false)
    private LocalDate appointmentDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AppointmentStatus status;

    @Column(name = "cancellation_reason", length = 400)
    private String cancellationReason;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    @Column(name = "previous_appointment_date")
    private LocalDate previousAppointmentDate;

    @Column(name = "previous_start_time")
    private LocalTime previousStartTime;

    @Column(name = "previous_end_time")
    private LocalTime previousEndTime;

    @Column(name = "reschedule_reason", length = 400)
    private String rescheduleReason;

    @Column(name = "rescheduled_at")
    private OffsetDateTime rescheduledAt;

    @Column(name = "reminder_requested_at")
    private OffsetDateTime reminderRequestedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Long doctorId) {
        this.doctorId = doctorId;
    }

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public LocalDate getAppointmentDate() {
        return appointmentDate;
    }

    public void setAppointmentDate(LocalDate appointmentDate) {
        this.appointmentDate = appointmentDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public void setStatus(AppointmentStatus status) {
        this.status = status;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public OffsetDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(OffsetDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDate getPreviousAppointmentDate() {
        return previousAppointmentDate;
    }

    public void setPreviousAppointmentDate(LocalDate previousAppointmentDate) {
        this.previousAppointmentDate = previousAppointmentDate;
    }

    public LocalTime getPreviousStartTime() {
        return previousStartTime;
    }

    public void setPreviousStartTime(LocalTime previousStartTime) {
        this.previousStartTime = previousStartTime;
    }

    public LocalTime getPreviousEndTime() {
        return previousEndTime;
    }

    public void setPreviousEndTime(LocalTime previousEndTime) {
        this.previousEndTime = previousEndTime;
    }

    public String getRescheduleReason() {
        return rescheduleReason;
    }

    public void setRescheduleReason(String rescheduleReason) {
        this.rescheduleReason = rescheduleReason;
    }

    public OffsetDateTime getRescheduledAt() {
        return rescheduledAt;
    }

    public void setRescheduledAt(OffsetDateTime rescheduledAt) {
        this.rescheduledAt = rescheduledAt;
    }

    public OffsetDateTime getReminderRequestedAt() {
        return reminderRequestedAt;
    }

    public void setReminderRequestedAt(OffsetDateTime reminderRequestedAt) {
        this.reminderRequestedAt = reminderRequestedAt;
    }
}
