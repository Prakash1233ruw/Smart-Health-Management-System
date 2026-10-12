package com.pp.smarthealth.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import com.pp.smarthealth.model.HealthTimelineTypes.*;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonInclude;

/** API boundaries: no JPA entities, credentials, or patient identity fields. */
public final class HealthTimelineDtos {
    private HealthTimelineDtos() { }
    public record ChangeAnalysisRequest(
            @Size(max = 20) List<@NotBlank @Size(max = 200) String> symptoms) { }
    public record SymptomChanges(List<String> newSymptoms, List<String> persistentSymptoms,
            List<String> resolvedSymptoms, boolean previousRecordAvailable) { }
    public record MetricChange(String metric, String unit, BigDecimal previousValue,
            BigDecimal latestValue, BigDecimal absoluteChange, BigDecimal percentageChange,
            ChangeDirection direction, LocalDateTime previousRecordedAt, LocalDateTime latestRecordedAt,
            String previousUnit) { }
    public record VerifiedChanges(boolean symptomsSubmitted, List<String> currentSymptoms,
            SymptomChanges symptomChanges, List<MetricChange> metricChanges) { }
    public record ChangeNarrative(
            @NotBlank @Size(max = 3000) String summary,
            @NotNull @Size(max = 10) List<@NotBlank @Size(max = 400) String> importantChanges,
            @NotNull @Size(max = 10) List<@NotBlank @Size(max = 400) String> itemsForDoctorReview,
            @NotNull @Size(max = 10) List<@NotBlank @Size(max = 400) String> questionsForDoctor) { }
    public record ChangeAnalysisResponse(Long analysisId, ReviewStatus reviewStatus,
            NarrativeStatus narrativeStatus, VerifiedChanges changes, ChangeNarrative narrative,
            List<String> dataConsidered, List<String> insufficientData,
            boolean requiresProfessionalReview, String disclaimer) implements TimelineDetails { }
    public record DoctorReviewRequest(
            @NotNull ReviewStatus status,
            @Size(max = 3000) String internalNotes,
            @Size(max = 1000) String followUpRecommendation,
            @FutureOrPresent LocalDate recommendedFollowUpDate) { }
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record DoctorReviewResponse(Long reviewId, Long analysisId, Long doctorId,
            ReviewStatus status, String internalNotes, String followUpRecommendation,
            LocalDate recommendedFollowUpDate, LocalDateTime reviewedAt) implements TimelineDetails { }
    public interface TimelineDetails { }
    public record MetricsDetails(LocalDate date, LocalDateTime timestamp,
            Map<String, BigDecimal> values, String unitNotice, Map<String,String> units) implements TimelineDetails { }
    public record SymptomAnalysisDetails(List<String> symptoms,
            SymptomAnalysisResponse analysis) implements TimelineDetails { }
    public record FollowUpDetails(Long analysisId, String recommendation,
            LocalDate recommendedDate) implements TimelineDetails { }
    public record TimelineEntryResponse(Long eventId, TimelineEventType eventType,
            LocalDateTime eventTime, String title, TimelineDetails details, ReviewStatus reviewStatus) { }
    public record PatientTimelineResponse(Long patientId, List<TimelineEntryResponse> entries,
            int page, int size, long totalElements, int totalPages) { }
}
