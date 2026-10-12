package com.pp.smarthealth.controller;

import com.pp.smarthealth.dto.ReportImportDtos.*;
import com.pp.smarthealth.dto.HealthMetricsDTO;
import com.pp.smarthealth.service.ReportImportService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name="app.legacy-ai-enabled", havingValue="true")
@RestController @RequestMapping("/api/report-imports") @PreAuthorize("hasRole('DOCTOR')")
public class ReportImportController {
    private final ReportImportService service;
    public ReportImportController(ReportImportService service) { this.service=service; }
    @PostMapping(consumes="multipart/form-data")
    public DraftView upload(@RequestParam Long patientId,@RequestPart("file") MultipartFile file,Authentication auth) { return service.upload(patientId,file,auth); }
    @GetMapping("/{id}") public DraftView get(@PathVariable Long id,Authentication auth) { return service.get(id,auth); }
    @PostMapping("/{id}/confirm") public HealthMetricsDTO confirm(@PathVariable Long id,@Valid @RequestBody ConfirmRequest request,Authentication auth) { return service.confirm(id,request,auth); }
}
