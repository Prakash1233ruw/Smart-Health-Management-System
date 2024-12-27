package com.pp.smarthealth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pp.smarthealth.model.HealthMetrics;

@Repository
public interface HealthMetricsRepository extends JpaRepository<HealthMetrics, Long> {
}