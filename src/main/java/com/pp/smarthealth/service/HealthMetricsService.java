package com.pp.smarthealth.service;

import com.pp.smarthealth.dto.HealthMetricsDTO;

import java.util.List;

public interface HealthMetricsService {

    HealthMetricsDTO saveHealthMetrics(HealthMetricsDTO healthMetricsDTO);

    List<HealthMetricsDTO> getAllHealthMetrics();

    HealthMetricsDTO getHealthMetricsById(Long id);

    HealthMetricsDTO addMissingUnits(Long id, java.util.Map<String, String> units, boolean verified);

    List<HealthMetricsDTO> getHealthMetricsByPatientId(Long patientId);
    
    boolean isAccessAllowed(Long patientId, String username);

	List<HealthMetricsDTO> getHealthMetricsByUsername(String name);
}
