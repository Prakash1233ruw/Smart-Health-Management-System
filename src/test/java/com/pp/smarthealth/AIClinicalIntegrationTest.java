package com.pp.smarthealth;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import com.pp.smarthealth.config.ScheduledTasks;
import com.pp.smarthealth.model.*;
import com.pp.smarthealth.repository.*;
import com.pp.smarthealth.service.ClinicalAIClient;
import com.pp.smarthealth.util.JwtUtil;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
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

@org.springframework.test.context.TestPropertySource(properties="app.legacy-ai-enabled=true")
@SpringBootTest(properties = "ai.save-history=true")
@AutoConfigureMockMvc
@Transactional
class AIClinicalIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JwtUtil jwt;
    @Autowired PatientRepository patients;
    @Autowired DoctorRepository doctors;
    @Autowired HealthMetricsRepository metrics;
    @Autowired AIConsultationRepository consultations;
    @Autowired EntityManager entityManager;
    @MockitoBean ClinicalAIClient client;
    @MockitoBean ScheduledTasks scheduledTasks;

    @Test void jwtToDatabaseFlowPersistsValidatedHistoryAndKeepsPatientDeletionWorking() throws Exception {
        Patient patient = patient("patient-integration");
        for (int i = 1; i <= 7; i++) {
            HealthMetrics metric = new HealthMetrics();
            metric.setPatient(patient);
            metric.setDate(LocalDate.of(2026, 1, i));
            metric.setTimestamp(LocalDateTime.of(2026, 1, i, 10, 0));
            metric.setHeartRate(70 + i);
            metrics.saveAndFlush(metric);
        }
        assertThat(metrics.findTop5ByPatientOrderByTimestampDescIdDesc(patient))
                .extracting(HealthMetrics::getHeartRate).containsExactly(77, 76, 75, 74, 73);
        when(client.analyze(anyString(), anyString())).thenReturn(validResponse());
        mvc.perform(post("/api/ai/symptom-analysis")
                        .header("Authorization", "Bearer " + jwt.generateToken(patient.getUsername()))
                        .contentType(MediaType.APPLICATION_JSON).content(body(patient.getId())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.riskLevel").value("MODERATE"))
                .andExpect(jsonPath("$.disclaimer").exists());
        consultations.flush();
        entityManager.clear();
        List<AIConsultation> saved = consultations.findAll();
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getCreatedAt()).isNotNull();
        assertThat(saved.get(0).getPatient().getId()).isEqualTo(patient.getId());
        assertThat(saved.get(0).getAiResponse()).contains("disclaimer", "MODERATE");
        ArgumentCaptor<String> context = ArgumentCaptor.forClass(String.class);
        verify(client).analyze(anyString(), context.capture());
        assertThat(context.getValue()).contains("2026-01-07").doesNotContain("2026-01-01", "2026-01-02");

        patients.deleteById(patient.getId());
        patients.flush();
        entityManager.clear();
        assertThat(consultations.count()).isZero();
    }

    @Test void patientCannotAnalyzeAnotherPatientAndDoctorCan() throws Exception {
        Patient owner = patient("owner");
        Patient other = patient("other");
        mvc.perform(post("/api/ai/symptom-analysis")
                        .header("Authorization", "Bearer " + jwt.generateToken(owner.getUsername()))
                        .contentType(MediaType.APPLICATION_JSON).content(body(other.getId())))
                .andExpect(status().isForbidden());
        verifyNoInteractions(client);

        Doctor doctor = new Doctor();
        doctor.setName("Demo Doctor");
        doctor.setUsername("doctor-integration");
        doctor.setEmail("doctor@example.test");
        doctor.setPassword("unused-test-password");
        doctor.setSpecialization("General medicine");
        doctors.saveAndFlush(doctor);
        when(client.analyze(anyString(), anyString())).thenReturn(validResponse());
        mvc.perform(post("/api/ai/symptom-analysis")
                        .header("Authorization", "Bearer " + jwt.generateToken(doctor.getUsername()))
                        .contentType(MediaType.APPLICATION_JSON).content(body(other.getId())))
                .andExpect(status().isOk());
    }

    @Test void swaggerIncludesEndpointAndBearerAuthentication() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/ai/symptom-analysis'].post.responses['200']").exists())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"));
    }

    private Patient patient(String username) {
        Patient patient = new Patient();
        patient.setName("Demo Patient");
        patient.setUsername(username);
        patient.setPassword("unused-test-password");
        patient.setEmail(username + "@example.test");
        patient.setAge(30);
        return patients.saveAndFlush(patient);
    }
    private String body(Long id) { return "{\"patientId\":" + id + ",\"symptoms\":[\"cough\"]}"; }
    private String validResponse() {
        return """
                {"riskLevel":"MODERATE","possibleConditions":["Possible respiratory infection"],
                 "recommendedTests":[],"recommendedSpecialist":"Primary care clinician",
                 "warningSigns":[],"explanation":"Seek professional evaluation. AI-generated decision support only."}
                """;
    }
}
