package com.pp.smarthealth.dto;

import java.util.List;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SymptomAnalysisRequest {
    @NotNull @Positive
    @Schema(example = "1")
    private Long patientId;

    @NotEmpty @Size(max = 20)
    @Schema(example = "[\"fever\",\"cough\",\"shortness of breath\"]")
    private List<@NotBlank @Size(max = 200) String> symptoms;
}
