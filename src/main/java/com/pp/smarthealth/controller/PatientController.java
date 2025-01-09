package com.pp.smarthealth.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pp.smarthealth.dto.HealthMetricsDTO;
import com.pp.smarthealth.dto.PatientDTO;
import com.pp.smarthealth.exception.ErrorDetails;
import com.pp.smarthealth.exception.HealthMetricsNotFoundException;
import com.pp.smarthealth.exception.PatientNotFoundException;
import com.pp.smarthealth.model.Patient;
import com.pp.smarthealth.service.HealthMetricsService;
import com.pp.smarthealth.service.PatientService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/patients")
@Tag(name = "Patient API", description = "API for managing patient records, including CRUD operations, and searching by name, age, or medical condition.")
public class PatientController {

    @Autowired
    private PatientService patientService;
    
    @Autowired
    private HealthMetricsService healthMetricsService;
    
    @Operation(summary = "POST Operation ", description = "adding a patient")
    @PostMapping("/register")
    public ResponseEntity<PatientDTO> createPatient(@RequestBody Patient patient) {
        PatientDTO createdPatient = patientService.savePatient(patient);
        return ResponseEntity.ok(createdPatient);
    }

    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<PatientDTO> getPatientById(@PathVariable Long id) {
        PatientDTO patient = patientService.getPatientById(id);
        return ResponseEntity.ok(patient);
    }
    
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<PatientDTO>> getAllPatients() {
        List<PatientDTO> patients = patientService.getAllPatients();
        return ResponseEntity.ok(patients);
    }

    
    @PutMapping("/{id}")
    public ResponseEntity<PatientDTO> updatePatient(@PathVariable Long id, @RequestBody Patient patientDetails) {
        PatientDTO updatedPatient = patientService.updatePatient(id, patientDetails);
        return ResponseEntity.ok(updatedPatient);
    }

    
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePatient(@PathVariable Long id) {
        patientService.deletePatient(id);
        return ResponseEntity.noContent().build();
    }
    
    
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    @GetMapping("/name/{name}")
    public ResponseEntity<List<PatientDTO>> getPatientsWithName(@PathVariable String name) {
    	List<PatientDTO> patients = patientService.searchPatientsWithName(name);
       return  new ResponseEntity<List<PatientDTO>>(patients,HttpStatus.OK);        
    }
    
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    @GetMapping("/age/{age}")
    public ResponseEntity<?> getPatientsWithAge(@PathVariable int age) {
    	List<PatientDTO> patients = patientService.searchPatientsWithAge(age);
    	return  new ResponseEntity<List<PatientDTO>>(patients,HttpStatus.OK); 
    }
    
    
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    @GetMapping("/medicalCondition/{medicalCondition}")
    public ResponseEntity<List<PatientDTO>> getPatientsWithMedicalCondition(@PathVariable String medicalCondition) {
    	List<PatientDTO> patients = patientService.searchPatientsMedicalCondition(medicalCondition);
    	return  new ResponseEntity<List<PatientDTO>>(patients,HttpStatus.OK); 
    }
    
   
   

}
