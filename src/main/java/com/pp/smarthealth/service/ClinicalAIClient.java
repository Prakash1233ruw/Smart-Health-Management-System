package com.pp.smarthealth.service;

public interface ClinicalAIClient {
    String analyze(String systemPrompt, String patientContext);
    default String summarizeVisit(String systemPrompt, String doctorNotes) {
        throw com.pp.smarthealth.exception.AIServiceException.unavailable();
    }
    default String summarizeChanges(String systemPrompt, String verifiedContext) {
        throw com.pp.smarthealth.exception.AIServiceException.unavailable();
    }
}
