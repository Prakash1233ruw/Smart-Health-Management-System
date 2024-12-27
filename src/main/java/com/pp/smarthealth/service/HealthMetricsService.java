package com.pp.smarthealth.service;

import com.pp.smarthealth.dto.HealthMetricsDTO;
import com.pp.smarthealth.model.HealthMetrics;

import java.util.List;

public interface HealthMetricsService {
    HealthMetricsDTO saveHealthMetrics(HealthMetrics healthMetrics);
    HealthMetricsDTO getHealthMetricsById(Long id);
    List<HealthMetricsDTO> getAllHealthMetrics();
    HealthMetricsDTO updateHealthMetrics(Long id, HealthMetrics healthMetricsDetails);
    void deleteHealthMetrics(Long id);
}
