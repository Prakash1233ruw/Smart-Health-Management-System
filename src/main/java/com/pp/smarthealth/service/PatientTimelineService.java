package com.pp.smarthealth.service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.pp.smarthealth.config.HealthTimelineProperties;
import com.pp.smarthealth.dto.HealthTimelineDtos.*;
import com.pp.smarthealth.dto.SymptomAnalysisResponse;
import com.pp.smarthealth.exception.*;
import com.pp.smarthealth.model.*;
import com.pp.smarthealth.model.HealthTimelineTypes.*;
import com.pp.smarthealth.repository.*;
import com.pp.smarthealth.repository.HealthChangeAnalysisRepository.TimelineRow;
import com.pp.smarthealth.security.PatientAccessService;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PatientTimelineService {
    private final PatientAccessService access;
    private final HealthMetricsRepository metrics;
    private final AIConsultationRepository consultations;
    private final HealthChangeAnalysisRepository analyses;
    private final DoctorReviewRepository reviews;
    private final HealthChangeCalculator calculator;
    private final ClinicalJson json;
    private final HealthTimelineProperties settings;
    public PatientTimelineService(PatientAccessService access, HealthMetricsRepository metrics,
            AIConsultationRepository consultations, HealthChangeAnalysisRepository analyses,
            DoctorReviewRepository reviews, HealthChangeCalculator calculator, ClinicalJson json,
            HealthTimelineProperties settings) {
        this.access = access; this.metrics = metrics; this.consultations = consultations;
        this.analyses = analyses; this.reviews = reviews; this.calculator = calculator;
        this.json = json; this.settings = settings;
    }
    @Transactional(readOnly = true)
    public PatientTimelineResponse me(Authentication auth, int page, int size) {
        return timeline(access.currentPatient(auth).getId(), false, page, size);
    }
    @Transactional(readOnly = true)
    public PatientTimelineResponse patient(Long patientId, Authentication auth, int page, int size) {
        return timeline(access.doctorPatient(patientId, auth).getId(), true, page, size);
    }
    private PatientTimelineResponse timeline(Long patientId, boolean doctor, int page, int size) {
        if (page < 0 || page > 100000 || size < 1 || size > settings.getMaxPageSize()) {
            throw new BadRequestException("Invalid pagination: page must be 0-100000; size must be 1-" + settings.getMaxPageSize());
        }
        try {
            var slice = analyses.timeline(patientId, PageRequest.of(page, size));
            List<TimelineRow> rows = slice.getContent();
            Map<Long, HealthMetrics> metricMap = index(metrics.findAllById(ids(rows, TimelineEventType.HEALTH_METRICS_RECORDED)), HealthMetrics::getId);
            Map<Long, AIConsultation> oldMap = index(consultations.findAllById(ids(rows, TimelineEventType.SYMPTOM_ANALYSIS)), AIConsultation::getId);
            Map<Long, HealthChangeAnalysis> analysisMap = index(analyses.findAllById(ids(rows, TimelineEventType.AI_CHANGE_ANALYSIS)), HealthChangeAnalysis::getId);
            Set<Long> reviewIds = new HashSet<>(ids(rows, TimelineEventType.DOCTOR_REVIEW));
            reviewIds.addAll(ids(rows, TimelineEventType.FOLLOW_UP_RECOMMENDED));
            Map<Long, DoctorReview> reviewMap = index(reviews.findAllById(reviewIds), DoctorReview::getId);
            List<TimelineEntryResponse> entries = new ArrayList<>();
            for (TimelineRow row : rows) {
                TimelineEventType type = TimelineEventType.valueOf(row.getEventType());
                TimelineDetails details;
                ReviewStatus status = null;
                String title;
                switch (type) {
                    case HEALTH_METRICS_RECORDED -> {
                        HealthMetrics m = required(metricMap, row.getEventId());
                        details = new MetricsDetails(m.getDate(), m.getTimestamp(), calculator.values(m),
                                m.getUnits()==null || m.getUnits().isEmpty() ? "Units were not recorded in the source; do not assume units."
                                    : "Recorded units are shown. Do not assume units for any remaining readings.", m.getUnits());
                        title = "Health measurements recorded";
                    }
                    case SYMPTOM_ANALYSIS -> {
                        AIConsultation c = required(oldMap, row.getEventId());
                        details = new SymptomAnalysisDetails(json.symptoms(c.getSymptoms()),
                                json.stored(c.getAiResponse(), SymptomAnalysisResponse.class));
                        title = "Symptom decision support";
                    }
                    case AI_CHANGE_ANALYSIS -> {
                        HealthChangeAnalysis a = required(analysisMap, row.getEventId());
                        ChangeAnalysisResponse stored = json.stored(a.getResponseJson(), ChangeAnalysisResponse.class);
                        status = a.getReviewStatus();
                        details = new ChangeAnalysisResponse(a.getId(), status, NarrativeStatus.AVAILABLE,
                                stored.changes(), stored.narrative(), stored.dataConsidered(), stored.insufficientData(),
                                true, SymptomAnalysisResponse.DISCLAIMER);
                        title = "AI summary of verified health changes";
                    }
                    case DOCTOR_REVIEW -> {
                        DoctorReview r = required(reviewMap, row.getEventId());
                        status = r.getStatus();
                        // Private fields are omitted from the patient projection, not hidden after entity serialization.
                        details = new DoctorReviewResponse(r.getId(), r.getAnalysis().getId(), r.getReviewerDoctorId(),
                                r.getStatus(), doctor ? r.getInternalNotes() : null,
                                r.getFollowUpRecommendation(), r.getRecommendedFollowUpDate(), r.getReviewedAt());
                        title = "Doctor review";
                    }
                    case FOLLOW_UP_RECOMMENDED -> {
                        DoctorReview r = required(reviewMap, row.getEventId());
                        status = r.getStatus();
                        details = new FollowUpDetails(r.getAnalysis().getId(), r.getFollowUpRecommendation(), r.getRecommendedFollowUpDate());
                        title = "Follow-up recommended";
                    }
                    default -> throw new TimelineStorageException();
                }
                entries.add(new TimelineEntryResponse(row.getEventId(), type, row.getEventTime(), title, details, status));
            }
            return new PatientTimelineResponse(patientId, List.copyOf(entries), page, size,
                    slice.getTotalElements(), slice.getTotalPages());
        } catch (DataAccessException ex) { throw new TimelineStorageException(); }
    }
    private List<Long> ids(List<TimelineRow> rows, TimelineEventType type) {
        return rows.stream().filter(r -> type.name().equals(r.getEventType())).map(TimelineRow::getEventId).toList();
    }
    private <T> Map<Long, T> index(List<T> values, Function<T, Long> id) {
        return values.stream().collect(Collectors.toMap(id, Function.identity()));
    }
    private <T> T required(Map<Long, T> values, Long id) {
        T result = values.get(id);
        if (result == null) { throw new TimelineStorageException(); }
        return result;
    }
}
