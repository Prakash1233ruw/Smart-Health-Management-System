package com.pp.smarthealth.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pp.smarthealth.config.AIProperties;
import com.pp.smarthealth.dto.*;
import com.pp.smarthealth.exception.*;
import com.pp.smarthealth.model.*;
import com.pp.smarthealth.repository.*;
import com.pp.smarthealth.service.impl.AIClinicalServiceImpl;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AIClinicalServiceTest {
    public static final String VALID_RESPONSE = """
            {"riskLevel":"HIGH","possibleConditions":["Possible respiratory infection"],
             "recommendedTests":["Clinical examination"],"recommendedSpecialist":"Primary care clinician",
             "warningSigns":["Worsening difficulty breathing"],
             "explanation":"AI-generated decision support requiring professional medical review."}
            """;
    @Mock PatientRepository patients;
    @Mock HealthMetricsRepository metrics;
    @Mock AIConsultationRepository consultations;
    @Mock ClinicalAIClient client;
    AIProperties properties;
    AIClinicalService service;
    Patient patient;
    ValidatorFactory validatorFactory;
    ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setup() {
        properties = new AIProperties();
        validatorFactory = Validation.buildDefaultValidatorFactory();
        service = new AIClinicalServiceImpl(patients, metrics, consultations, client, properties,
                mapper, validatorFactory.getValidator());
        patient = new Patient();
        patient.setId(1L);
        patient.setUsername("patient-one");
        patient.setName("Private Name");
        patient.setEmail("private@example.test");
        patient.setPassword("private-password");
        patient.setAge(34);
        patient.setMedicalCondition("Asthma");
    }
    @AfterEach void closeValidator() { validatorFactory.close(); }

    private Authentication auth(String role) {
        return new UsernamePasswordAuthenticationToken("patient-one", null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    }
    private SymptomAnalysisRequest request() {
        return new SymptomAnalysisRequest(1L, List.of(" fever ", "cough"));
    }
    private void existingPatient() {
        when(patients.findById(1L)).thenReturn(Optional.of(patient));
        when(metrics.findTop5ByPatientOrderByTimestampDescIdDesc(patient)).thenReturn(List.of());
    }

    @Test void validOwnPatientUsesClinicalContextAndSavesValidatedHistory() throws Exception {
        properties.setSaveHistory(true);
        when(patients.findByUsername("patient-one")).thenReturn(Optional.of(patient));
        when(patients.findById(1L)).thenReturn(Optional.of(patient));
        HealthMetrics metric = new HealthMetrics();
        metric.setTimestamp(LocalDateTime.of(2026, 1, 1, 9, 0));
        metric.setBloodOxygenLevel(94);
        metric.setPatient(patient);
        when(metrics.findTop5ByPatientOrderByTimestampDescIdDesc(patient)).thenReturn(List.of(metric));
        when(client.analyze(anyString(), anyString())).thenReturn(VALID_RESPONSE);

        var result = service.analyze(request(), auth("PATIENT"));
        assertThat(result.getRiskLevel()).isEqualTo(SymptomAnalysisResponse.RiskLevel.HIGH);
        assertThat(result.getDisclaimer()).contains("not a diagnosis", "qualified healthcare professional");
        ArgumentCaptor<String> context = ArgumentCaptor.forClass(String.class);
        verify(client).analyze(contains("Do not prescribe medications or dosages"), context.capture());
        assertThat(context.getValue()).contains("Asthma", "94", "2026-01-01")
                .doesNotContain("Private Name", "private@example.test", "private-password", "patient-one");
        assertThat(mapper.readTree(context.getValue()).get("age").asInt()).isEqualTo(34);
        ArgumentCaptor<AIConsultation> saved = ArgumentCaptor.forClass(AIConsultation.class);
        verify(consultations).save(saved.capture());
        assertThat(saved.getValue().getPatient()).isSameAs(patient);
        assertThat(saved.getValue().getSymptoms()).isEqualTo("[\"fever\",\"cough\"]");
        assertThat(saved.getValue().getAiResponse()).contains("disclaimer");
    }

    @Test void doctorCanAnalyzePatientWithoutMetricsAndHistoryDefaultsOff() {
        existingPatient();
        patient.setAge(0);
        patient.setMedicalCondition(null);
        when(client.analyze(anyString(), anyString())).thenReturn(VALID_RESPONSE);
        service.analyze(request(), auth("DOCTOR"));
        verify(client).analyze(anyString(), contains("\"age\":null"));
        verifyNoInteractions(consultations);
    }

    @Test void missingPatientReturnsNotFoundForDoctor() {
        when(patients.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.analyze(request(), auth("DOCTOR")))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(client, metrics, consultations);
    }

    @Test void anotherPatientIsDeniedBeforeTargetLookup() {
        patient.setId(2L);
        when(patients.findByUsername("patient-one")).thenReturn(Optional.of(patient));
        assertThatThrownBy(() -> service.analyze(request(), auth("PATIENT")))
                .isInstanceOf(AccessDeniedException.class);
        verify(patients, never()).findById(anyLong());
        verifyNoInteractions(client, metrics, consultations);
    }

    @Test void adminAndAnonymousAreDenied() {
        assertThatThrownBy(() -> service.analyze(request(), auth("ADMIN")))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.analyze(request(), null))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(patients, client, metrics, consultations);
    }

    @Test void invalidRequestsDoNotReadPatientOrCallAI() {
        for (var invalid : List.of(new SymptomAnalysisRequest(1L, List.of()),
                new SymptomAnalysisRequest(0L, List.of("fever")),
                new SymptomAnalysisRequest(null, List.of("fever")),
                new SymptomAnalysisRequest(1L, List.of(" ")),
                new SymptomAnalysisRequest(1L, List.of("x".repeat(201))),
                new SymptomAnalysisRequest(1L, java.util.Collections.nCopies(21, "cough")))) {
            assertThatThrownBy(() -> service.analyze(invalid, auth("PATIENT")))
                    .isInstanceOf(BadRequestException.class);
        }
        verifyNoInteractions(patients, client, metrics, consultations);
    }

    @Test void providerFailureIsPropagatedWithoutSaving() {
        existingPatient();
        when(client.analyze(anyString(), anyString())).thenThrow(AIServiceException.unavailable());
        assertThatThrownBy(() -> service.analyze(request(), auth("DOCTOR")))
                .isInstanceOf(AIServiceException.class).hasMessage("AI analysis is currently unavailable.");
        verifyNoInteractions(consultations);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"not JSON", "{}", "null", "[]", "{\"riskLevel\":\"UNKNOWN\"}"})
    void malformedResponsesAreRejected(String raw) {
        existingPatient();
        when(client.analyze(anyString(), anyString())).thenReturn(raw);
        assertThatThrownBy(() -> service.analyze(request(), auth("DOCTOR")))
                .isInstanceOf(AIServiceException.class)
                .hasMessage("AI analysis returned an invalid or incomplete response.");
        verifyNoInteractions(consultations);
    }

    @Test void structurallyWrongOrOutOfBoundsResponsesAreRejected() {
        existingPatient();
        for (String raw : List.of(VALID_RESPONSE + " {}",
                VALID_RESPONSE.replace("\"HIGH\"", "2"),
                VALID_RESPONSE.replace("\"HIGH\"", "\"HIGH\",\"riskLevel\":\"LOW\""),
                VALID_RESPONSE.replace("\"recommendedTests\":[\"Clinical examination\"]", "\"recommendedTests\":null"),
                VALID_RESPONSE.replace("\"Clinical examination\"", "42"),
                VALID_RESPONSE.replace("Primary care clinician", " "),
                VALID_RESPONSE.replace("Primary care clinician", "x".repeat(201)),
                VALID_RESPONSE.replace("\"riskLevel\"", "\"unexpected\":true,\"riskLevel\""))) {
            when(client.analyze(anyString(), anyString())).thenReturn(raw);
            assertThatThrownBy(() -> service.analyze(request(), auth("DOCTOR")))
                    .isInstanceOf(AIServiceException.class);
        }
        verifyNoInteractions(consultations);
    }

    @Test void storageFailuresDoNotExposeDatabaseDetails() {
        existingPatient();
        properties.setSaveHistory(true);
        when(client.analyze(anyString(), anyString())).thenReturn(VALID_RESPONSE);
        when(consultations.save(any())).thenThrow(new org.springframework.dao.DataIntegrityViolationException("secret SQL"));
        assertThatThrownBy(() -> service.analyze(request(), auth("DOCTOR")))
                .isInstanceOf(AIServiceException.class).hasMessage("Analysis history could not be saved.");
    }
}
