package com.pp.smarthealth.controller;

import com.pp.smarthealth.dto.HealthTimelineDtos.PatientTimelineResponse;
import com.pp.smarthealth.service.PatientTimelineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/health-timeline")
@Tag(name = "Patient Health Timeline")
@SecurityRequirement(name = "bearerAuth")
public class HealthTimelineController {
    private final PatientTimelineService service;
    public HealthTimelineController(PatientTimelineService service) { this.service = service; }
    @GetMapping("/me")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "View the authenticated patient's timeline", description = "Newest first; page >= 0, size 1-100. Private doctor notes are omitted.")
    public PatientTimelineResponse me(Authentication auth,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return service.me(auth, page, size);
    }
    @GetMapping("/patients/{patientId}")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(summary = "Doctor views a patient's timeline", description = "Uses the existing role-wide doctor access policy; includes internal review notes.")
    public PatientTimelineResponse patient(@PathVariable Long patientId, Authentication auth,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return service.patient(patientId, auth, page, size);
    }
}
