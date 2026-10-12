package com.pp.smarthealth.service;

import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pp.smarthealth.dto.HealthTimelineDtos.*;
import com.pp.smarthealth.exception.TimelineStorageException;
import com.pp.smarthealth.model.*;
import com.pp.smarthealth.model.HealthTimelineTypes.*;
import com.pp.smarthealth.repository.HealthChangeAnalysisRepository;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class ChangeAnalysisStoreTest {
    @Test void databaseFailureCannotBeReportedAsSuccessfulAnalysis() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var repository = mock(HealthChangeAnalysisRepository.class);
            when(repository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("private DB detail"));
            var store = new ChangeAnalysisStore(repository, new ClinicalJson(new ObjectMapper(), factory.getValidator()));
            var response = new ChangeAnalysisResponse(null, ReviewStatus.PENDING_REVIEW, NarrativeStatus.AVAILABLE,
                    new VerifiedChanges(true, List.of("cough"), new SymptomChanges(List.of("cough"), List.of(), List.of(), false), List.of()),
                    new ChangeNarrative("Summary", List.of(), List.of(), List.of()), List.of(), List.of(), true, "Disclaimer");
            assertThatThrownBy(() -> store.save(new Patient(), response, "test-model"))
                    .isInstanceOf(TimelineStorageException.class).hasMessageNotContaining("private DB detail");
        }
    }
}
