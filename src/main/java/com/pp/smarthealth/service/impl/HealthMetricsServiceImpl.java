package com.pp.smarthealth.service.impl;

import com.pp.smarthealth.dto.HealthMetricsDTO;
import com.pp.smarthealth.model.HealthMetrics;
import com.pp.smarthealth.repository.HealthMetricsRepository;
import com.pp.smarthealth.service.HealthMetricsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class HealthMetricsServiceImpl implements HealthMetricsService {

    @Autowired
    private HealthMetricsRepository healthMetricsRepository;

    private HealthMetricsDTO convertToDTO(HealthMetrics healthMetrics) {
        HealthMetricsDTO healthMetricsDTO = new HealthMetricsDTO();
        healthMetricsDTO.setId(healthMetrics.getId());
        healthMetricsDTO.setDate(healthMetrics.getDate());
        healthMetricsDTO.setTimestamp(healthMetrics.getTimestamp());
        healthMetricsDTO.setSystolicPressure(healthMetrics.getSystolicPressure());
        healthMetricsDTO.setDiastolicPressure(healthMetrics.getDiastolicPressure());
        healthMetricsDTO.setHeartRate(healthMetrics.getHeartRate());
        healthMetricsDTO.setTemperature(healthMetrics.getTemperature());
        healthMetricsDTO.setRespiratoryRate(healthMetrics.getRespiratoryRate());
        healthMetricsDTO.setBloodOxygenLevel(healthMetrics.getBloodOxygenLevel());
        healthMetricsDTO.setBloodGlucoseLevel(healthMetrics.getBloodGlucoseLevel());
        healthMetricsDTO.setBmi(healthMetrics.getBmi());
        healthMetricsDTO.setTotalCholesterol(healthMetrics.getTotalCholesterol());
        healthMetricsDTO.setHdlCholesterol(healthMetrics.getHdlCholesterol());
        healthMetricsDTO.setLdlCholesterol(healthMetrics.getLdlCholesterol());
        healthMetricsDTO.setTriglycerides(healthMetrics.getTriglycerides());
        healthMetricsDTO.setWeight(healthMetrics.getWeight());
        healthMetricsDTO.setBloodPressure(healthMetrics.getBloodPressure());
        healthMetricsDTO.setHeight(healthMetrics.getHeight());
        healthMetricsDTO.setWaistCircumference(healthMetrics.getWaistCircumference());
        healthMetricsDTO.setPhysicalActivityLevel(healthMetrics.getPhysicalActivityLevel());
        return healthMetricsDTO;
    }

    @Override
    public HealthMetricsDTO saveHealthMetrics(HealthMetrics healthMetrics) {
        HealthMetrics savedHealthMetrics = healthMetricsRepository.save(healthMetrics);
        return convertToDTO(savedHealthMetrics);
    }

    @Override
    public HealthMetricsDTO getHealthMetricsById(Long id) {
        HealthMetrics healthMetrics = findHealthMetricsById(id);
        return convertToDTO(healthMetrics);
    }

    private HealthMetrics findHealthMetricsById(Long id) {
        return healthMetricsRepository.findById(id).orElseThrow(() -> new RuntimeException("HealthMetrics not found"));
    }

    @Override
    public List<HealthMetricsDTO> getAllHealthMetrics() {
        return healthMetricsRepository.findAll().stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    public HealthMetricsDTO updateHealthMetrics(Long id, HealthMetrics healthMetricsDetails) {
        HealthMetrics healthMetrics = findHealthMetricsById(id);
        healthMetrics.setDate(healthMetricsDetails.getDate());
        healthMetrics.setTimestamp(healthMetricsDetails.getTimestamp());
        healthMetrics.setSystolicPressure(healthMetricsDetails.getSystolicPressure());
        healthMetrics.setDiastolicPressure(healthMetricsDetails.getDiastolicPressure());
        healthMetrics.setHeartRate(healthMetricsDetails.getHeartRate());
        healthMetrics.setTemperature(healthMetricsDetails.getTemperature());
        healthMetrics.setRespiratoryRate(healthMetricsDetails.getRespiratoryRate());
        healthMetrics.setBloodOxygenLevel(healthMetricsDetails.getBloodOxygenLevel());
        healthMetrics.setBloodGlucoseLevel(healthMetricsDetails.getBloodGlucoseLevel());
        healthMetrics.setBmi(healthMetricsDetails.getBmi());
        healthMetrics.setTotalCholesterol(healthMetricsDetails.getTotalCholesterol());
        healthMetrics.setHdlCholesterol(healthMetricsDetails.getHdlCholesterol());
        healthMetrics.setLdlCholesterol(healthMetricsDetails.getLdlCholesterol());
        healthMetrics.setTriglycerides(healthMetricsDetails.getTriglycerides());
        healthMetrics.setWeight(healthMetricsDetails.getWeight());
        healthMetrics.setBloodPressure(healthMetricsDetails.getBloodPressure());
        healthMetrics.setHeight(healthMetricsDetails.getHeight());
        healthMetrics.setWaistCircumference(healthMetricsDetails.getWaistCircumference());
        healthMetrics.setPhysicalActivityLevel(healthMetricsDetails.getPhysicalActivityLevel());
        HealthMetrics updatedHealthMetrics = healthMetricsRepository.save(healthMetrics);
        return convertToDTO(updatedHealthMetrics);
    }

    @Override
    public void deleteHealthMetrics(Long id) {
        HealthMetrics healthMetrics = findHealthMetricsById(id);
        healthMetricsRepository.delete(healthMetrics);
    }
}
