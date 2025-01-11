package com.pp.smarthealth.controller;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pp.smarthealth.dto.AppointmentDTO;
import com.pp.smarthealth.model.Patient;
import com.pp.smarthealth.service.AppointmentService;
import com.pp.smarthealth.service.PatientService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/appointments")
@Tag(name = "Appointment API", description = "API for managing appointments, including scheduling, updating, and viewing appointment history.")
public class AppointmentController {

    @Autowired
    private AppointmentService appointmentService;
    
    @Autowired
    private PatientService patientService;
    
    @Operation(summary = "POST Operation ", description = "scheduling the appointment")
    @PostMapping
    public ResponseEntity<?> createAppointment(@RequestBody AppointmentDTO appointmentDTO) {
    	 Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    	 String username=null;
         if (authentication != null && authentication.isAuthenticated()) {
              username = authentication.getName();
         }else {
        	   return ResponseEntity.status(403).body("patient not authenticated!");
         }
         
         Long patientId= patientService.findPatientIdByUsername(username);
         
    	appointmentDTO.setPatientId(patientId);
        AppointmentDTO createdAppointment = appointmentService.createAppointment(appointmentDTO);
        return ResponseEntity.ok(createdAppointment);
    }
     
    @PutMapping("/{id}")
    public ResponseEntity<AppointmentDTO> updateAppointment(@PathVariable Long id, @RequestBody AppointmentDTO appointmentDTO) {
        AppointmentDTO updatedAppointment = appointmentService.updateAppointment(id, appointmentDTO);
        return ResponseEntity.ok(updatedAppointment);
    }

    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<AppointmentDTO> getAppointmentById(@PathVariable Long id) {
        AppointmentDTO appointmentDTO = appointmentService.getAppointmentById(id);
        return ResponseEntity.ok(appointmentDTO);
    }

    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<AppointmentDTO>> getAllAppointments() {
        List<AppointmentDTO> appointments = appointmentService.getAllAppointments();
        return ResponseEntity.ok(appointments);
    }

//    @DeleteMapping("/{id}")
//    public ResponseEntity<Void> deleteAppointment(@PathVariable Long id) {
//        appointmentService.deleteAppointment(id);
//        return ResponseEntity.noContent().build();
//    }
//    
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    @PostMapping("/send-reminders")
    @Operation(summary = "Send appointment reminders", description = "Send email reminders for upcoming appointments")
    public ResponseEntity<Void> sendAppointmentReminders() {
        appointmentService.sendAppointmentReminders();
        return ResponseEntity.ok().build();
    }
    
    @Operation(summary = "GET Operation", description = "List all appointments of the logged-in patient")
    @GetMapping("/myappointments")
    public ResponseEntity<List<AppointmentDTO>> getMyAppointments() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = null;

        if (authentication != null && authentication.isAuthenticated()) {
            username = authentication.getName();
        } else {
            return ResponseEntity.status(403).body(Collections.emptyList());
        }

        Patient patient = patientService.findPatientByUsername(username);
        if (patient == null) {
            return ResponseEntity.status(404).body(Collections.emptyList());
        }

        List<AppointmentDTO> appointments = appointmentService.findAppointmentsByPatient(patient);
        return ResponseEntity.ok(appointments);
    }
    
    
    @Operation(summary = "DELETE Operation", description = "Cancel an appointment")
    @DeleteMapping("/{appointmentId}")
    public ResponseEntity<?> cancelAppointment(@PathVariable Long appointmentId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = null;

        if (authentication != null && authentication.isAuthenticated()) {
            username = authentication.getName();
        } else {
            return ResponseEntity.status(403).body("User not authenticated!");
        }

        Long patientId = patientService.findPatientIdByUsername(username);
        if (patientId == null) {
            return ResponseEntity.status(404).body("Patient ID not found for the user: " + username);
        }

        boolean isCanceled = appointmentService.cancelAppointment(appointmentId, patientId);
        if (isCanceled) {
            return ResponseEntity.ok("Appointment canceled successfully.");
        } else {
            return ResponseEntity.status(403).body("You are not authorized to cancel this appointment.");
        }
    }


}
