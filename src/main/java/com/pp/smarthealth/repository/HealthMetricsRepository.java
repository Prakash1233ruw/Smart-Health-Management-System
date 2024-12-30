package com.pp.smarthealth.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pp.smarthealth.model.HealthMetrics;
import com.pp.smarthealth.model.Patient;

@Repository
public interface HealthMetricsRepository extends JpaRepository<HealthMetrics, Long> {

    List<HealthMetrics> findByPatient(Patient patient);
}