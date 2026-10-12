package com.pp.smarthealth.service;

import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pp.smarthealth.dto.HealthTimelineDtos.ChangeNarrative;
import com.pp.smarthealth.exception.AIServiceException;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class ClinicalJsonTest {
    @Test void rejectsOversizedListsStringsDuplicatesAndScalarCoercion() throws Exception {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var mapper = new ObjectMapper();
            var parser = new ClinicalJson(mapper, factory.getValidator());
            var valid = new ChangeNarrative("Verified summary", List.of(), List.of(), List.of());
            assertThat(parser.narrative(mapper.writeValueAsString(valid), ChangeNarrative.class)).isEqualTo(valid);
            for (var invalid : List.of(
                    new ChangeNarrative("x".repeat(3001), List.of(), List.of(), List.of()),
                    new ChangeNarrative("x", java.util.Collections.nCopies(11, "item"), List.of(), List.of()),
                    new ChangeNarrative("x", List.of("x".repeat(401)), List.of(), List.of()),
                    new ChangeNarrative("x", List.of(" "), List.of(), List.of()))) {
                assertThatThrownBy(() -> parser.narrative(mapper.writeValueAsString(invalid), ChangeNarrative.class))
                        .isInstanceOf(AIServiceException.class);
            }
            for (String raw : List.of(
                    "{\"summary\":5,\"importantChanges\":[],\"itemsForDoctorReview\":[],\"questionsForDoctor\":[]}",
                    "{\"summary\":\"x\",\"summary\":\"y\",\"importantChanges\":[],\"itemsForDoctorReview\":[],\"questionsForDoctor\":[]}",
                    "{\"summary\":\"x\",\"importantChanges\":[],\"itemsForDoctorReview\":[],\"questionsForDoctor\":[]} {}",
                    " ".repeat(24001))) {
                assertThatThrownBy(() -> parser.narrative(raw, ChangeNarrative.class)).isInstanceOf(AIServiceException.class);
            }
        }
    }
}
