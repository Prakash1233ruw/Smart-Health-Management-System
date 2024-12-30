package com.pp.smarthealth.service;

import com.pp.smarthealth.dto.HealthMetricsDTO;

import java.util.List;

public interface HealthMetricsService {

    HealthMetricsDTO saveHealthMetrics(HealthMetricsDTO healthMetricsDTO);

    List<HealthMetricsDTO> getAllHealthMetrics();

    HealthMetricsDTO getHealthMetricsById(Long id);

    List<HealthMetricsDTO> getHealthMetricsByPatientId(Long patientId);
}
