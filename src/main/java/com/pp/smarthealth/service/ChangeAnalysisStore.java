package com.pp.smarthealth.service;

import com.pp.smarthealth.dto.HealthTimelineDtos.*;
import com.pp.smarthealth.exception.*;
import com.pp.smarthealth.model.*;
import com.pp.smarthealth.repository.*;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The remote call completes before entering this short persistence transaction. */
@Service
public class ChangeAnalysisStore {
    private final HealthChangeAnalysisRepository analyses;
    private final ClinicalJson json;
    public ChangeAnalysisStore(HealthChangeAnalysisRepository analyses, ClinicalJson json) {
        this.analyses = analyses;
        this.json = json;
    }
    @Transactional
    public HealthChangeAnalysis save(Patient patient, ChangeAnalysisResponse response, String model) {
        HealthChangeAnalysis analysis = new HealthChangeAnalysis();
        analysis.setPatient(patient);
        analysis.setModelName(model);
        analysis.setSymptoms(response.changes().symptomsSubmitted() ? json.write(response.changes().currentSymptoms()) : null);
        analysis.setResponseJson(json.write(response));
        try { return analyses.saveAndFlush(analysis); }
        catch (DataAccessException ex) { throw new TimelineStorageException(); }
    }
}
