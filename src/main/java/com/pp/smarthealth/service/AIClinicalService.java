package com.pp.smarthealth.service;

import com.pp.smarthealth.dto.SymptomAnalysisRequest;
import com.pp.smarthealth.dto.SymptomAnalysisResponse;
import org.springframework.security.core.Authentication;

public interface AIClinicalService {
    SymptomAnalysisResponse analyze(SymptomAnalysisRequest request, Authentication authentication);
}
