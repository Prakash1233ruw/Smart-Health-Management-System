package com.pp.smarthealth.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public final class ReportImportDtos {
    private ReportImportDtos() {}
    public record ExtractedRow(@NotNull @Size(max=64) String metric, @NotNull @Size(max=80) String value,
        @NotNull @Size(max=40) String unit, @NotNull @Size(max=300) String sourceText, @Min(1) @Max(5) int page) {}
    public record Extraction(@Size(max=10) String reportDate,
        @NotNull @Size(max=20) List<@Valid ExtractedRow> rows,
        @NotNull @Size(max=10) List<@NotBlank @Size(max=300) String> warnings) {}
    public record DraftRow(int index, String metric, String value, String unit, String sourceText,
        int page, boolean supported, String evidenceStatus) {}
    public record DraftView(Long draftId, Long patientId, String patientName, String filename,
        LocalDate reportDate, List<DraftRow> rows, List<String> warnings,
        Map<String,List<String>> allowedUnits, String status, Long measurementId) {}
    public record ConfirmRow(@Min(0) @Max(19) int index, @NotBlank @Size(max=64) String metric,
        @NotNull @DecimalMin("0") @DecimalMax("1000000") BigDecimal value,
        @NotBlank @Size(max=40) String unit) {}
    public record ConfirmRequest(@NotNull @PastOrPresent LocalDate recordedDate,
        @AssertTrue boolean verifiedAgainstReport,
        @NotNull @Size(min=1,max=16) List<@Valid ConfirmRow> rows) {}
}
