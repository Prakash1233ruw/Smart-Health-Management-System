package com.pp.smarthealth.controller;

import com.pp.smarthealth.dto.HealthTimelineDtos.*;
import com.pp.smarthealth.service.HealthChangeAnalysisService;
import com.pp.smarthealth.service.DoctorReviewService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name="app.legacy-ai-enabled", havingValue="true")
@RestController
@RequestMapping("/api/ai")
@Tag(name = "AI Health Changes and Doctor Review")
@SecurityRequirement(name = "bearerAuth")
public class HealthChangeAnalysisController {
    private final HealthChangeAnalysisService service;
    private final DoctorReviewService reviews;
    public HealthChangeAnalysisController(HealthChangeAnalysisService service, DoctorReviewService reviews) {
        this.service = service; this.reviews = reviews;
    }
    @PostMapping("/change-analysis/me")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Analyze health changes for the authenticated patient",
            description = "Java calculates changes. Gemini only summarizes. Omit symptoms for a metrics-only analysis; an empty array explicitly reports no current symptoms. A 200 response may contain UNAVAILABLE or NOT_REQUESTED narrativeStatus and no saved analysis.")
    public ChangeAnalysisResponse me(@Valid @RequestBody ChangeAnalysisRequest request, Authentication auth) {
        return service.analyzeMe(request, auth);
    }
    @PostMapping("/change-analysis/patients/{patientId}")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(summary = "Doctor requests a patient's health-change analysis")
    public ChangeAnalysisResponse patient(@PathVariable Long patientId,
            @Valid @RequestBody ChangeAnalysisRequest request, Authentication auth) {
        return service.analyzePatient(patientId, request, auth);
    }
    @PatchMapping("/analyses/{analysisId}/review")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(summary = "Append a doctor review",
            description = "PENDING_REVIEW -> REVIEWED/FOLLOW_UP_REQUIRED/DISMISSED; FOLLOW_UP_REQUIRED -> REVIEWED/DISMISSED. Terminal reviews cannot be edited. Follow-up requires a patient-visible recommendation.")
    public DoctorReviewResponse review(@PathVariable Long analysisId,
            @Valid @RequestBody DoctorReviewRequest request, Authentication auth) {
        return reviews.review(analysisId, request, auth);
    }
}
