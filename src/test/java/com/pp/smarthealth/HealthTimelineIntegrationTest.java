package com.pp.smarthealth;

import java.time.LocalDate;
import java.time.LocalDateTime;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pp.smarthealth.config.ScheduledTasks;
import com.pp.smarthealth.exception.AIServiceException;
import com.pp.smarthealth.model.*;
import com.pp.smarthealth.repository.*;
import com.pp.smarthealth.service.ClinicalAIClient;
import com.pp.smarthealth.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@org.springframework.test.context.TestPropertySource(properties="app.legacy-ai-enabled=true")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class HealthTimelineIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JwtUtil jwt;
    @Autowired ObjectMapper json;
    @Autowired PatientRepository patients;
    @Autowired DoctorRepository doctors;
    @Autowired HealthMetricsRepository metrics;
    @Autowired HealthChangeAnalysisRepository analyses;
    @Autowired DoctorReviewRepository reviews;
    @Autowired AIConsultationRepository consultations;
    @MockitoBean ClinicalAIClient client;
    @MockitoBean ScheduledTasks scheduledTasks;
    @MockitoBean com.pp.smarthealth.service.impl.EmailService emailService;
    Patient patient;
    Doctor doctor;
    String patientToken;
    String doctorToken;
    static final String NARRATIVE = """
            {"summary":"Recorded changes need professional review.","importantChanges":["Heart rate changed."],
            "itemsForDoctorReview":["Confirm measurements."],"questionsForDoctor":["When should readings be repeated?"]}
            """;

    @BeforeEach void setup() {
        patient = new Patient(); patient.setName("Timeline Patient"); patient.setUsername("timeline-patient");
        patient.setEmail("timeline-patient@example.test"); patient.setPassword("unused"); patient.setAge(30);
        patient = patients.saveAndFlush(patient);
        doctor = new Doctor(); doctor.setName("Timeline Doctor"); doctor.setUsername("timeline-doctor");
        doctor.setEmail("timeline-doctor@example.test"); doctor.setPassword("unused"); doctor.setSpecialization("General");
        doctor = doctors.saveAndFlush(doctor);
        patientToken = "Bearer " + jwt.generateToken(patient.getUsername());
        doctorToken = "Bearer " + jwt.generateToken(doctor.getUsername());
        when(client.summarizeChanges(anyString(), anyString())).thenReturn(NARRATIVE);
    }
    @Test void completeFlowPaginatesAndKeepsPrivateDoctorNotesOutOfPatientTimeline() throws Exception {
        metric(1, 60); metric(2, 75);
        long id = analyze("""
{"symptoms":["cough"]}""");
        mvc.perform(patch("/api/ai/analyses/{id}/review", id).header("Authorization", doctorToken)
                .contentType(MediaType.APPLICATION_JSON).content("""
                    {"status":"FOLLOW_UP_REQUIRED","internalNotes":"PRIVATE CLINICAL NOTE",
                    "followUpRecommendation":"Arrange a clinic appointment."}
                    """)).andExpect(status().isOk());
        var patientResult = mvc.perform(get("/api/health-timeline/me").header("Authorization", patientToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(5))
                .andReturn().getResponse().getContentAsString();
        assertThat(patientResult).doesNotContain("internalNotes", "PRIVATE CLINICAL NOTE", "password", "email");
        assertThat(patientResult).contains("FOLLOW_UP_RECOMMENDED", "Arrange a clinic appointment.", "FOLLOW_UP_REQUIRED");
        mvc.perform(get("/api/health-timeline/patients/{id}", patient.getId()).header("Authorization", doctorToken))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("PRIVATE CLINICAL NOTE")));
        mvc.perform(get("/api/health-timeline/me?page=1&size=1").header("Authorization", patientToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.entries.length()").value(1))
                .andExpect(jsonPath("$.totalPages").value(5));
        ArgumentCaptor<String> context = ArgumentCaptor.forClass(String.class);
        verify(client).summarizeChanges(anyString(), context.capture());
        assertThat(context.getValue()).contains("absoluteChange", "15").doesNotContain(patient.getEmail(), patient.getUsername());
        assertThat(analyses.count()).isEqualTo(1);
        assertThat(reviews.count()).isEqualTo(1);
    }
    @Test void successiveSymptomsUseSavedBaselineAndNormalizeDuplicates() throws Exception {
        analyze("""
{"symptoms":["cough","fever"]}""");
        mvc.perform(post("/api/ai/change-analysis/me").header("Authorization", patientToken)
                .contentType(MediaType.APPLICATION_JSON).content("""
{"symptoms":["COUGH","fatigue","fatigue"]}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.changes.symptomChanges.persistentSymptoms[0]").value("COUGH"))
                .andExpect(jsonPath("$.changes.symptomChanges.newSymptoms[0]").value("fatigue"))
                .andExpect(jsonPath("$.changes.symptomChanges.resolvedSymptoms[0]").value("fever"));
    }
    @Test void providerFailureReturnsVerifiedChangesAndDoesNotPersistInvalidAnalysis() throws Exception {
        metric(1, 60); metric(2, 75);
        when(client.summarizeChanges(anyString(), anyString())).thenThrow(AIServiceException.unavailable());
        mvc.perform(post("/api/ai/change-analysis/me").header("Authorization", patientToken)
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.narrativeStatus").value("UNAVAILABLE"))
                .andExpect(jsonPath("$.analysisId").isEmpty())
                .andExpect(jsonPath("$.changes.metricChanges[2].absoluteChange").value(15));
        assertThat(analyses.count()).isZero();
    }
    @ParameterizedTest
    @ValueSource(strings = {"not-json", "{}", "null",
            "{\"summary\":\"x\",\"importantChanges\":[],\"itemsForDoctorReview\":[],\"questionsForDoctor\":[],\"absoluteChange\":999}"})
    void malformedOrInventedProviderFieldsCannotOverrideJavaValues(String response) throws Exception {
        when(client.summarizeChanges(anyString(), anyString())).thenReturn(response);
        mvc.perform(post("/api/ai/change-analysis/me").header("Authorization", patientToken)
                .contentType(MediaType.APPLICATION_JSON).content("""
{"symptoms":["cough"]}"""))
                .andExpect(status().isOk()).andExpect(jsonPath("$.narrativeStatus").value("UNAVAILABLE"));
        assertThat(analyses.count()).isZero();
    }
    @Test void missingDataSkipsProviderAndReportsInsufficientData() throws Exception {
        mvc.perform(post("/api/ai/change-analysis/me").header("Authorization", patientToken)
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.narrativeStatus").value("NOT_REQUESTED"))
                .andExpect(jsonPath("$.insufficientData").isArray());
        verifyNoInteractions(client);
    }
    @Test void roleAndMappedIdentityChecksApplyBeforeAnyProviderCall() throws Exception {
        mvc.perform(get("/api/health-timeline/patients/{id}", patient.getId()).header("Authorization", patientToken))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/ai/change-analysis/patients/{id}", patient.getId()).header("Authorization", patientToken)
                .contentType(MediaType.APPLICATION_JSON).content("""
{"symptoms":["cough"]}"""))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/ai/analyses/1/review").header("Authorization", patientToken)
                .contentType(MediaType.APPLICATION_JSON).content("""
{"status":"REVIEWED"}"""))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/health-timeline/patients/{id}", patient.getId()).with(user("unmapped").roles("DOCTOR")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/health-timeline/me")).andExpect(status().isUnauthorized());
        verifyNoInteractions(client);
    }
    @ParameterizedTest @ValueSource(strings = {"page=-1", "page=bad", "size=0", "size=101", "page=100001"})
    void paginationIsBounded(String query) throws Exception {
        mvc.perform(get("/api/health-timeline/me?" + query).header("Authorization", patientToken))
                .andExpect(status().isBadRequest());
    }
    @Test void reviewsAppendHistoryAndTerminalStatusCannotBeRewritten() throws Exception {
        long id = analyze("""
{"symptoms":["cough"]}""");
        review(id, """
{"status":"FOLLOW_UP_REQUIRED","followUpRecommendation":"Book a review."}""", 200);
        review(id, """
{"status":"REVIEWED","internalNotes":"Reviewed at appointment."}""", 200);
        review(id, """
{"status":"DISMISSED"}""", 409);
        assertThat(reviews.count()).isEqualTo(2);
        assertThat(analyses.findById(id).orElseThrow().getReviewStatus()).isEqualTo(HealthTimelineTypes.ReviewStatus.REVIEWED);
    }
    @Test void invalidReviewInputsAndMissingAnalysisAreRejected() throws Exception {
        long id = analyze("""
{"symptoms":["cough"]}""");
        review(id, """
{"status":"PENDING_REVIEW"}""", 409);
        review(id, """
{"status":"FOLLOW_UP_REQUIRED"}""", 400);
        review(id, """
{"status":"REVIEWED","recommendedFollowUpDate":"2000-01-01"}""", 400);
        review(id, """
{"status":"UNKNOWN"}""", 400);
        review(999999, """
{"status":"REVIEWED"}""", 404);
        assertThat(reviews.count()).isZero();
    }
    @Test void doctorCanInitiateAnalysisAndUnknownPatientIsNotFound() throws Exception {
        mvc.perform(post("/api/ai/change-analysis/patients/{id}", patient.getId()).header("Authorization", doctorToken)
                .contentType(MediaType.APPLICATION_JSON).content("""
{"symptoms":["cough"]}"""))
                .andExpect(status().isOk()).andExpect(jsonPath("$.reviewStatus").value("PENDING_REVIEW"));
        mvc.perform(get("/api/health-timeline/patients/999999").header("Authorization", doctorToken))
                .andExpect(status().isNotFound());
    }
    @Test void legacySymptomHistoryIsVisibleAndCanSupplyComparisonBaseline() throws Exception {
        var legacy = new AIConsultation(); legacy.setPatient(patient); legacy.setSymptoms("[\"cough\",\"fatigue\"]");
        legacy.setRiskLevel(com.pp.smarthealth.dto.SymptomAnalysisResponse.RiskLevel.LOW);
        legacy.setAiResponse("""
                {"riskLevel":"LOW","possibleConditions":[],"recommendedTests":[],
                "recommendedSpecialist":"Primary care clinician","warningSigns":[],"explanation":"Professional review required."}
                """);
        consultations.saveAndFlush(legacy);
        mvc.perform(post("/api/ai/change-analysis/me").header("Authorization", patientToken)
                .contentType(MediaType.APPLICATION_JSON).content("""
                {"symptoms":["cough","fever"]}
                """)).andExpect(status().isOk())
                .andExpect(jsonPath("$.changes.symptomChanges.resolvedSymptoms[0]").value("fatigue"));
        mvc.perform(get("/api/health-timeline/me").header("Authorization", patientToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.entries[1].eventType").value("SYMPTOM_ANALYSIS"))
                .andExpect(jsonPath("$.entries[1].details.analysis.riskLevel").value("LOW"))
                .andExpect(jsonPath("$.entries[1].details.analysis.disclaimer").exists());
        assertThat(consultations.findById(legacy.getId()).orElseThrow().getAiResponse()).isEqualTo(legacy.getAiResponse());
    }
    @Test void anotherPatientReceivesOnlyTheirOwnEmptyTimeline() throws Exception {
        analyze("""
                {"symptoms":["cough"]}
                """);
        var other = new Patient(); other.setUsername("timeline-other"); other.setName("Other");
        other.setPassword("unused"); other.setEmail("timeline-other@example.test"); other.setAge(30);
        other = patients.saveAndFlush(other);
        mvc.perform(get("/api/health-timeline/me?patientId=" + patient.getId())
                .header("Authorization", "Bearer " + jwt.generateToken(other.getUsername())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.patientId").value(other.getId()))
                .andExpect(jsonPath("$.totalElements").value(0));
    }
    @Test void existingPatientDoctorMetricAndAppointmentApisStillWork() throws Exception {
        mvc.perform(get("/api/patients/{id}", patient.getId()).header("Authorization", doctorToken)).andExpect(status().isOk());
        mvc.perform(get("/api/doctors/{id}", doctor.getId()).with(user("regression-admin").roles("ADMIN"))).andExpect(status().isOk());
        mvc.perform(post("/api/healthmetrics").header("Authorization", doctorToken)
                .contentType(MediaType.APPLICATION_JSON).content("""
                {"patientId":%d,"date":"2026-01-01","timestamp":"2026-01-01T10:00:00","heartRate":60}
                """.formatted(patient.getId()))).andExpect(status().isOk());
        mvc.perform(get("/api/healthmetrics/myhealthreports").header("Authorization", patientToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].heartRate").value(60));
        var appointment = mvc.perform(post("/api/appointments").header("Authorization", patientToken)
                .contentType(MediaType.APPLICATION_JSON).content("""
                {"doctorId":%d,"dateTime":"2030-01-01T10:00:00","status":"SCHEDULED","notes":"Demo appointment"}
                """.formatted(doctor.getId()))).andExpect(status().isOk()).andReturn();
        long appointmentId = json.readTree(appointment.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(get("/api/appointments/{id}", appointmentId).header("Authorization", doctorToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.patientId").value(patient.getId()));
        verifyNoInteractions(client);
    }
    private long analyze(String body) throws Exception {
        var result = mvc.perform(post("/api/ai/change-analysis/me").header("Authorization", patientToken)
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.narrativeStatus").value("AVAILABLE"))
                .andExpect(jsonPath("$.reviewStatus").value("PENDING_REVIEW"))
                .andExpect(jsonPath("$.requiresProfessionalReview").value(true)).andReturn();
        return json.readTree(result.getResponse().getContentAsString()).get("analysisId").asLong();
    }
    private void review(long id, String body, int expectedStatus) throws Exception {
        mvc.perform(patch("/api/ai/analyses/{id}/review", id).header("Authorization", doctorToken)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().is(expectedStatus));
    }
    private void metric(int day, int heart) {
        var metric = new HealthMetrics(); metric.setPatient(patient);
        metric.setDate(LocalDate.of(2026, 1, day)); metric.setTimestamp(LocalDateTime.of(2026, 1, day, 10, 0));
        metric.setHeartRate(heart); metrics.saveAndFlush(metric);
    }
}

