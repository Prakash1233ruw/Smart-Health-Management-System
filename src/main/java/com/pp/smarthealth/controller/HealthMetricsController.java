package com.pp.smarthealth.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pp.smarthealth.dto.HealthMetricsDTO;
import com.pp.smarthealth.model.HealthMetrics;
import com.pp.smarthealth.service.HealthMetricsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/healthmetrics")
@Tag(name = "Health Metrics API", description = "API for recording and tracking health metrics such as weight and blood pressure, and generating health reports.")
public class HealthMetricsController {

    @Autowired
    private HealthMetricsService healthMetricsService;

    @PostMapping
    @Operation(summary = "POST Operation ", description = "adding HealthMetrics for a patient")
    public ResponseEntity<HealthMetricsDTO> createHealthMetrics(@RequestBody HealthMetrics healthMetrics) {
        HealthMetricsDTO createdHealthMetrics = healthMetricsService.saveHealthMetrics(healthMetrics);
        return ResponseEntity.ok(createdHealthMetrics);
    }

    @GetMapping("/{id}")
    public ResponseEntity<HealthMetricsDTO> getHealthMetricsById(@PathVariable Long id) {
        HealthMetricsDTO healthMetrics = healthMetricsService.getHealthMetricsById(id);
        return ResponseEntity.ok(healthMetrics);
    }

    @GetMapping
    public ResponseEntity<List<HealthMetricsDTO>> getAllHealthMetrics() {
        List<HealthMetricsDTO> healthMetricsList = healthMetricsService.getAllHealthMetrics();
        return ResponseEntity.ok(healthMetricsList);
    }

    @PutMapping("/{id}")
    public ResponseEntity<HealthMetricsDTO> updateHealthMetrics(@PathVariable Long id, @RequestBody HealthMetrics healthMetricsDetails) {
        HealthMetricsDTO updatedHealthMetrics = healthMetricsService.updateHealthMetrics(id, healthMetricsDetails);
        return ResponseEntity.ok(updatedHealthMetrics);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHealthMetrics(@PathVariable Long id) {
        healthMetricsService.deleteHealthMetrics(id);
        return ResponseEntity.noContent().build();
    }
}
