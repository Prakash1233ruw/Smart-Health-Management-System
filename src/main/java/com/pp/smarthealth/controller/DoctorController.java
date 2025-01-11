package com.pp.smarthealth.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pp.smarthealth.dto.AppointmentDTO;
import com.pp.smarthealth.dto.DoctorDTO;
import com.pp.smarthealth.model.Appointment;
import com.pp.smarthealth.model.Doctor;
import com.pp.smarthealth.service.DoctorService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.websocket.server.PathParam;

@RestController
@RequestMapping("/api/doctors")
@Tag(name = "Doctor API", description = "API for managing doctor records, including CRUD operations and retrieving doctor information.")
public class DoctorController {

    @Autowired
    private DoctorService doctorService;
    
    @PostMapping("/register")
    public ResponseEntity<DoctorDTO> createDoctor(@RequestBody Doctor doctor) {
        DoctorDTO createdDoctor = doctorService.saveDoctor(doctor);
        return ResponseEntity.ok(createdDoctor);
    }

    
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<DoctorDTO> updateDoctor(@PathVariable Long id, @RequestBody Doctor doctorDetails) {
        DoctorDTO updatedDoctor = doctorService.updateDoctor(id, doctorDetails);
        return ResponseEntity.ok(updatedDoctor);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDoctor(@PathVariable Long id) {
        doctorService.deleteDoctor(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<DoctorDTO> getDoctorById(@PathVariable Long id) {
        DoctorDTO doctor = doctorService.getDoctorById(id);
        return ResponseEntity.ok(doctor);
    }
    
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    @GetMapping("/upcoming-appointments/{doctorId}")
    public ResponseEntity<List<AppointmentDTO>> getUpcomingAppointments(@PathVariable Long doctorId) {
        List<AppointmentDTO> appointments = doctorService.getUpcomingAppointments(doctorId);
        return ResponseEntity.ok(appointments);
    }
    
    @GetMapping("/myupcoming-appointments")
    public ResponseEntity<?> getUpcomingAppointments() {
        try {
            List<AppointmentDTO> appointments = doctorService.getUpcomingAppointments();
            return ResponseEntity.ok(appointments);
        } catch (RuntimeException e) {
            // Handle service layer errors
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<DoctorDTO>> getAllDoctors() {
        List<DoctorDTO> doctors = doctorService.getAllDoctors();
        return ResponseEntity.ok(doctors);
    }
}
