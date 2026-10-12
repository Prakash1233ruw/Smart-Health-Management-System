package com.pp.smarthealth.repository;

import com.pp.smarthealth.model.AIConsultation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AIConsultationRepository extends JpaRepository<AIConsultation, Long> {
    java.util.Optional<AIConsultation> findFirstByPatientIdOrderByCreatedAtDescIdDesc(Long patientId);
}
