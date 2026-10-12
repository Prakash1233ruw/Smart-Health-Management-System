package com.pp.smarthealth.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import jakarta.validation.constraints.*;
import com.pp.smarthealth.dto.HealthTimelineDtos.MetricChange;

public final class VisitDtos {
    private VisitDtos() { }
    public record Report(Long recordId, LocalDateTime recordedAt, Map<String, BigDecimal> values, Map<String, String> units) { }
    public record Flag(Long analysisId, LocalDateTime recordedAt, String status, String urgency) { }
    public record Brief(Long appointmentId, String patientName, String doctorName, LocalDateTime appointmentTime,
            LocalDateTime preparedAt, String reasonForVisit, String questions, List<MetricChange> trends,
            List<Report> latestReports, List<Flag> historicalFlags, List<String> missingData, String comparisonRule) { }
    public record VisitRow(Long id, Long doctorId, Long patientId, String doctorName, String patientName,
            LocalDateTime dateTime, String status, String notes, String questions, boolean summaryAvailable) { }
    public record DraftRequest(@NotBlank @Size(max=4000) String doctorNotes) { }
    public record Draft(String summary, boolean aiAvailable, String notice) { }
    public record ApproveRequest(@NotBlank @Size(max=3000) String summary,
            @NotNull @Size(max=12) List<@NotBlank @Size(max=300) String> actions,
            @FutureOrPresent LocalDate followUpDate,
            @NotNull @AssertTrue Boolean approve,
            @NotNull @AssertTrue Boolean visitCompleted) { }
    public record Summary(Long appointmentId, Long doctorId, String doctorName, String summary,
            List<String> actions, LocalDate followUpDate, LocalDateTime approvedAt) { }
    public record SummaryView(String status, Summary summary) { }
}
