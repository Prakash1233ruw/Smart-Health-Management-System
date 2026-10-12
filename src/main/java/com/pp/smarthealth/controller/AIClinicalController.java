package com.pp.smarthealth.controller;

import com.pp.smarthealth.dto.SymptomAnalysisRequest;
import com.pp.smarthealth.dto.SymptomAnalysisResponse;
import com.pp.smarthealth.service.AIClinicalService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name="app.legacy-ai-enabled", havingValue="true")
@RestController
@RequestMapping("/api/ai")
@Tag(name = "AI Clinical Decision Support")
public class AIClinicalController {
    private final AIClinicalService service;

    public AIClinicalController(AIClinicalService service) { this.service = service; }

    @PostMapping("/symptom-analysis")
    @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR')")
    @Operation(summary = "Analyze symptoms using AI decision support",
            description = "Patients may analyze only their own data. Doctors follow the existing "
                    + "role-wide patient access rule. Sends symptoms and available clinical context to Google Gemini. "
                    + "Not a diagnosis; requires qualified healthcare professional review.",
            security = @SecurityRequirement(name = "bearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    content = @Content(schema = @Schema(implementation = SymptomAnalysisRequest.class),
                            examples = @ExampleObject(value = """
                                    {"patientId":1,"symptoms":["fever","cough","shortness of breath"]}
                                    """))),
            responses = {
                @ApiResponse(responseCode = "200", description = "Validated AI decision support",
                        content = @Content(schema = @Schema(implementation = SymptomAnalysisResponse.class),
                                examples = @ExampleObject(value = """
                                        {"riskLevel":"HIGH","possibleConditions":["Possible respiratory infection"],
                                         "recommendedTests":["Clinical examination"],"recommendedSpecialist":"Primary care clinician",
                                         "warningSigns":["Worsening difficulty breathing"],
                                         "explanation":"AI-generated decision support; these symptoms may require medical evaluation by a qualified healthcare professional.",
                                         "disclaimer":"AI-generated decision support only; not a diagnosis. Review by a qualified healthcare professional is required."}
                                        """))),
                @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
                @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content),
                @ApiResponse(responseCode = "403", description = "Access denied", content = @Content),
                @ApiResponse(responseCode = "404", description = "Patient not found", content = @Content),
                @ApiResponse(responseCode = "502", description = "Invalid AI output", content = @Content),
                @ApiResponse(responseCode = "503", description = "AI or history storage unavailable", content = @Content),
                @ApiResponse(responseCode = "504", description = "AI timeout", content = @Content)
            })
    public SymptomAnalysisResponse analyze(@Valid @RequestBody SymptomAnalysisRequest request,
            Authentication authentication) {
        return service.analyze(request, authentication);
    }
}
