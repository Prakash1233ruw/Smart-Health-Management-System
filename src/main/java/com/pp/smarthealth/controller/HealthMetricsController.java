package com.pp.smarthealth.controller;

import com.pp.smarthealth.dto.HealthMetricsDTO;
import com.pp.smarthealth.service.HealthMetricsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/healthmetrics")
public class HealthMetricsController {

    @Autowired
    private HealthMetricsService healthMetricsService;

    @PostMapping
    public ResponseEntity<HealthMetricsDTO> saveHealthMetrics(@RequestBody HealthMetricsDTO healthMetricsDTO) {
        HealthMetricsDTO savedHealthMetrics = healthMetricsService.saveHealthMetrics(healthMetricsDTO);
        return ResponseEntity.ok(savedHealthMetrics);
    }

    @GetMapping
    public ResponseEntity<List<HealthMetricsDTO>> getAllHealthMetrics() {
        List<HealthMetricsDTO> healthMetricsList = healthMetricsService.getAllHealthMetrics();
        return ResponseEntity.ok(healthMetricsList);
    }

    @GetMapping("/{id}")
    public ResponseEntity<HealthMetricsDTO> getHealthMetricsById(@PathVariable Long id) {
        HealthMetricsDTO healthMetrics = healthMetricsService.getHealthMetricsById(id);
        return ResponseEntity.ok(healthMetrics);
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<HealthMetricsDTO>> getHealthMetricsByPatientId(@PathVariable Long patientId) {
        List<HealthMetricsDTO> healthMetricsList = healthMetricsService.getHealthMetricsByPatientId(patientId);
        return ResponseEntity.ok(healthMetricsList);
    }
}
