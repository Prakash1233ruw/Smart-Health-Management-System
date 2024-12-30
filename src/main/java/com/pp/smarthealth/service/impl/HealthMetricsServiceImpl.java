package com.pp.smarthealth.service.impl;

import com.pp.smarthealth.dto.HealthMetricsDTO;
import com.pp.smarthealth.dto.PatientDTO;
import com.pp.smarthealth.model.HealthMetrics;
import com.pp.smarthealth.model.Patient;
import com.pp.smarthealth.repository.HealthMetricsRepository;
import com.pp.smarthealth.repository.PatientRepository;
import com.pp.smarthealth.service.HealthMetricsService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class HealthMetricsServiceImpl implements HealthMetricsService {

    @Autowired
    private HealthMetricsRepository healthMetricsRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Override
    public HealthMetricsDTO saveHealthMetrics(HealthMetricsDTO healthMetricsDTO) {
        Patient patient = patientRepository.findById(healthMetricsDTO.getPatientId())
                .orElseThrow(() -> new RuntimeException("Patient not found"));

        HealthMetrics healthMetrics = convertToEntity(healthMetricsDTO, patient);
        HealthMetrics savedHealthMetrics = healthMetricsRepository.save(healthMetrics);
        return convertToDTO(savedHealthMetrics);
    }

    @Override
    public List<HealthMetricsDTO> getAllHealthMetrics() {
        return healthMetricsRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public HealthMetricsDTO getHealthMetricsById(Long id) {
        HealthMetrics healthMetrics = healthMetricsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("HealthMetrics not found"));
        return convertToDTO(healthMetrics);
    }

    @Override
    public List<HealthMetricsDTO> getHealthMetricsByPatientId(Long patientId) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found"));
        return healthMetricsRepository.findByPatient(patient)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private HealthMetricsDTO convertToDTO(HealthMetrics healthMetrics) {
        return new HealthMetricsDTO(
                healthMetrics.getId(),
                healthMetrics.getDate(),
                healthMetrics.getTimestamp(),
                healthMetrics.getSystolicPressure(),
                healthMetrics.getDiastolicPressure(),
                healthMetrics.getHeartRate(),
                healthMetrics.getTemperature(),
                healthMetrics.getRespiratoryRate(),
                healthMetrics.getBloodOxygenLevel(),
                healthMetrics.getBloodGlucoseLevel(),
                healthMetrics.getBmi(),
                healthMetrics.getTotalCholesterol(),
                healthMetrics.getHdlCholesterol(),
                healthMetrics.getLdlCholesterol(),
                healthMetrics.getTriglycerides(),
                healthMetrics.getWeight(),
                healthMetrics.getHeight(),
                healthMetrics.getWaistCircumference(),
                healthMetrics.getPhysicalActivityLevel(),
                healthMetrics.getPatient().getId() // Only passing patientId here
        );
    }

    private HealthMetrics convertToEntity(HealthMetricsDTO healthMetricsDTO, Patient patient) {
        return new HealthMetrics(
                healthMetricsDTO.getId(),
                healthMetricsDTO.getDate(),
                healthMetricsDTO.getTimestamp(),
                healthMetricsDTO.getSystolicPressure(),
                healthMetricsDTO.getDiastolicPressure(),
                healthMetricsDTO.getHeartRate(),
                healthMetricsDTO.getTemperature(),
                healthMetricsDTO.getRespiratoryRate(),
                healthMetricsDTO.getBloodOxygenLevel(),
                healthMetricsDTO.getBloodGlucoseLevel(),
                healthMetricsDTO.getBmi(),
                healthMetricsDTO.getTotalCholesterol(),
                healthMetricsDTO.getHdlCholesterol(),
                healthMetricsDTO.getLdlCholesterol(),
                healthMetricsDTO.getTriglycerides(),
                healthMetricsDTO.getWeight(),
                healthMetricsDTO.getHeight(),
                healthMetricsDTO.getWaistCircumference(),
                healthMetricsDTO.getPhysicalActivityLevel(),
                patient // Mapping the Patient object
        );
    }
}
