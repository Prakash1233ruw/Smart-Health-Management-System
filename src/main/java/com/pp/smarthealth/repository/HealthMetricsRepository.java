package com.pp.smarthealth.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pp.smarthealth.model.HealthMetrics;
import com.pp.smarthealth.model.Patient;

@Repository
public interface HealthMetricsRepository extends JpaRepository<HealthMetrics, Long> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select m from HealthMetrics m where m.id = :id")
    java.util.Optional<HealthMetrics> findForUnitUpdate(@org.springframework.data.repository.query.Param("id") Long id);

    List<HealthMetrics> findByPatient(Patient patient);

    List<HealthMetrics> findTop5ByPatientOrderByTimestampDescIdDesc(Patient patient);
    List<HealthMetrics> findByPatientIdOrderByTimestampDescIdDesc(Long patientId, org.springframework.data.domain.Pageable pageable);
}
