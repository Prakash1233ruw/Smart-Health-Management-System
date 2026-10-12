package com.pp.smarthealth.repository;

import java.time.LocalDateTime;
import java.util.Optional;
import com.pp.smarthealth.model.HealthChangeAnalysis;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface HealthChangeAnalysisRepository extends JpaRepository<HealthChangeAnalysis, Long> {
    java.util.List<HealthChangeAnalysis> findTop10ByPatientIdAndReviewStatusInOrderByCreatedAtDesc(Long patientId, java.util.Collection<com.pp.smarthealth.model.HealthTimelineTypes.ReviewStatus> statuses);
    Optional<HealthChangeAnalysis> findFirstByPatientIdAndSymptomsIsNotNullOrderByCreatedAtDescIdDesc(Long patientId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from HealthChangeAnalysis a where a.id = :id")
    Optional<HealthChangeAnalysis> findForReview(@Param("id") Long id);

    interface TimelineRow {
        Long getEventId();
        String getEventType();
        LocalDateTime getEventTime();
    }
    // The database pages the union; no unlimited source histories are loaded into memory.
    String EVENTS = """
            SELECT h.id AS event_id, 'HEALTH_METRICS_RECORDED' AS event_type, h.timestamp AS event_time
              FROM health_metrics h WHERE h.patient_id = :patientId
            UNION ALL
            SELECT c.id, 'SYMPTOM_ANALYSIS', c.created_at
              FROM ai_consultations c WHERE c.patient_id = :patientId
            UNION ALL
            SELECT a.id, 'AI_CHANGE_ANALYSIS', a.created_at
              FROM health_change_analyses a WHERE a.patient_id = :patientId
            UNION ALL
            SELECT r.id, 'DOCTOR_REVIEW', r.reviewed_at
              FROM doctor_reviews r JOIN health_change_analyses a ON a.id = r.analysis_id
              WHERE a.patient_id = :patientId
            UNION ALL
            SELECT r.id, 'FOLLOW_UP_RECOMMENDED', r.reviewed_at
              FROM doctor_reviews r JOIN health_change_analyses a ON a.id = r.analysis_id
              WHERE a.patient_id = :patientId
                AND (r.follow_up_recommendation IS NOT NULL OR r.recommended_follow_up_date IS NOT NULL)
            """;
    @Query(value = "SELECT e.event_id AS eventId, e.event_type AS eventType, e.event_time AS eventTime FROM ("
            + EVENTS + ") e ORDER BY e.event_time DESC, e.event_type DESC, e.event_id DESC",
            countQuery = "SELECT COUNT(*) FROM (" + EVENTS + ") e", nativeQuery = true)
    Page<TimelineRow> timeline(@Param("patientId") Long patientId, Pageable pageable);
}
