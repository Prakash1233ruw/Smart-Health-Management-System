package com.pp.smarthealth.dto;

import java.util.List;
import jakarta.validation.constraints.*;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
public class SymptomAnalysisResponse {
    public enum RiskLevel { LOW, MODERATE, HIGH, EMERGENCY }

    public static final String DISCLAIMER = "AI-generated decision support only; not a diagnosis. "
            + "Review by a qualified healthcare professional is required. "
            + "If you believe you are experiencing an emergency, seek immediate medical assistance; "
            + "do not wait for this analysis.";

    @NotNull
    @Schema(example = "HIGH")
    private RiskLevel riskLevel;
    @NotNull @Size(max = 10)
    @Schema(example = "[\"Possible respiratory infection\"]")
    private List<@NotBlank @Size(max = 300) String> possibleConditions;
    @NotNull @Size(max = 10)
    @Schema(example = "[\"Clinical examination\"]")
    private List<@NotBlank @Size(max = 300) String> recommendedTests;
    @NotBlank @Size(max = 200)
    @Schema(example = "Primary care clinician")
    private String recommendedSpecialist;
    @NotNull @Size(max = 10)
    @Schema(example = "[\"Worsening difficulty breathing\"]")
    private List<@NotBlank @Size(max = 300) String> warningSigns;
    @NotBlank @Size(max = 3000)
    @Schema(example = "These symptoms may require medical evaluation; this is not a diagnosis.")
    private String explanation;

    // Server-owned: the model cannot omit or replace the safety notice.
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getDisclaimer() { return DISCLAIMER; }
}
