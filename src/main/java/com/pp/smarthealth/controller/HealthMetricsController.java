package com.pp.smarthealth.controller;

import com.pp.smarthealth.dto.HealthMetricsDTO;
import com.pp.smarthealth.exception.ErrorDetails;
import com.pp.smarthealth.exception.HealthMetricsNotFoundException;
import com.pp.smarthealth.exception.PatientNotFoundException;
import com.pp.smarthealth.service.HealthMetricsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/healthmetrics")
public class HealthMetricsController {

    @Autowired
    private HealthMetricsService healthMetricsService;
 
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<HealthMetricsDTO> saveHealthMetrics(@RequestBody HealthMetricsDTO healthMetricsDTO) {
        HealthMetricsDTO savedHealthMetrics = healthMetricsService.saveHealthMetrics(healthMetricsDTO);
        return ResponseEntity.ok(savedHealthMetrics);
    }

    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<HealthMetricsDTO>> getAllHealthMetrics() {
        List<HealthMetricsDTO> healthMetricsList = healthMetricsService.getAllHealthMetrics();
        return ResponseEntity.ok(healthMetricsList);
    }

    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<HealthMetricsDTO> getHealthMetricsById(@PathVariable Long id) {
        HealthMetricsDTO healthMetrics = healthMetricsService.getHealthMetricsById(id);
        return ResponseEntity.ok(healthMetrics);
    }

    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<HealthMetricsDTO>> getHealthMetricsByPatientId(@PathVariable Long patientId) {
        List<HealthMetricsDTO> healthMetricsList = healthMetricsService.getHealthMetricsByPatientId(patientId);
        return ResponseEntity.ok(healthMetricsList);
    }
    
    @GetMapping("/myhealthreports")
    public ResponseEntity<?> getHealthReports(Principal principal) {
        try {
            
            List<HealthMetricsDTO> healthReports = healthMetricsService.getHealthMetricsByUsername(principal.getName());
            return ResponseEntity.ok(healthReports);
        } catch (PatientNotFoundException e) {
           
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorDetails("Patient not found", e.getMessage()));
        } catch (HealthMetricsNotFoundException e) {
           
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorDetails("No health reports found", e.getMessage()));
        } catch (Exception e) {
         
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorDetails("An unexpected error occurred", e.getMessage()));
        }
    }
}
