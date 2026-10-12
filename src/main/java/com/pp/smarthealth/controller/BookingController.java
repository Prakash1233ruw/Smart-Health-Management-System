package com.pp.smarthealth.controller;

import java.time.LocalDateTime;
import java.util.List;
import com.pp.smarthealth.dto.AppointmentDTO;
import com.pp.smarthealth.exception.ResourceNotFoundException;
import com.pp.smarthealth.repository.DoctorRepository;
import com.pp.smarthealth.security.PatientAccessService;
import com.pp.smarthealth.service.AppointmentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** Additive UI endpoints; existing admin doctor lists and appointment APIs stay intact. */
@RestController
@SecurityRequirement(name = "bearerAuth")
public class BookingController {
    private final DoctorRepository doctors;
    private final PatientAccessService access;
    private final AppointmentService appointments;
    public BookingController(DoctorRepository doctors, PatientAccessService access, AppointmentService appointments) {
        this.doctors = doctors; this.access = access; this.appointments = appointments;
    }
    public record DoctorDirectoryEntry(Long id, String name, String specialization) { }
    public record BookingRequest(
            @NotNull @Positive Long doctorId,
            @NotNull @Future LocalDateTime dateTime,
            @Size(max = 500) String notes,
            @Size(max = 1000) String questions) { }

    @GetMapping("/api/doctors/directory")
    @PreAuthorize("hasAnyRole('PATIENT','DOCTOR','ADMIN')")
    @Operation(summary = "Minimal doctor directory for booking", description = "No usernames, emails, or passwords.")
    public List<DoctorDirectoryEntry> directory() {
        return doctors.findAll().stream().map(d -> new DoctorDirectoryEntry(d.getId(), d.getName(), d.getSpecialization())).toList();
    }

    @PostMapping("/api/appointments/book")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Book for the authenticated patient", description = "Patient identity and Scheduled status are assigned by the server.")
    public AppointmentDTO book(@Valid @RequestBody BookingRequest request, Authentication auth) {
        var patient = access.currentPatient(auth);
        if (!doctors.existsById(request.doctorId())) { throw new ResourceNotFoundException("Doctor not found"); }
        var booking = new AppointmentDTO();
        booking.setPatientId(patient.getId());
        booking.setDoctorId(request.doctorId());
        booking.setDateTime(request.dateTime());
        booking.setStatus("Scheduled");
        booking.setNotes(request.notes() == null ? null : request.notes().trim());
        booking.setQuestions(request.questions() == null ? null : request.questions().trim());
        return appointments.createAppointment(booking);
    }
}
