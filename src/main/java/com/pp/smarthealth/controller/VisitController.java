package com.pp.smarthealth.controller;

import com.pp.smarthealth.dto.VisitDtos.*;
import com.pp.smarthealth.service.VisitService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/appointments")
public class VisitController {
    private final VisitService service;
    public VisitController(VisitService service) { this.service=service; }
    @GetMapping("/visits") @PreAuthorize("hasAnyRole('PATIENT','DOCTOR')")
    public List<VisitRow> visits(Authentication auth) { return service.mine(auth); }
    @GetMapping("/{id}/brief") @PreAuthorize("hasRole('DOCTOR')")
    public Brief brief(@PathVariable Long id,Authentication auth) { return service.brief(id,auth); }
    @PostMapping("/{id}/summary/draft") @PreAuthorize("hasRole('DOCTOR')")
    public Draft draft(@PathVariable Long id,@Valid @RequestBody DraftRequest request,Authentication auth) { return service.draft(id,request,auth); }
    @PostMapping("/{id}/summary") @PreAuthorize("hasRole('DOCTOR')")
    public Summary approve(@PathVariable Long id,@Valid @RequestBody ApproveRequest request,Authentication auth) { return service.approve(id,request,auth); }
    @GetMapping("/{id}/summary") @PreAuthorize("hasAnyRole('PATIENT','DOCTOR')")
    public SummaryView summary(@PathVariable Long id,Authentication auth) { return service.summary(id,auth); }
}
