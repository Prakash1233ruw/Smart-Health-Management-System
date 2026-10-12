package com.pp.smarthealth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pp.smarthealth.config.ScheduledTasks;
import com.pp.smarthealth.model.*;
import com.pp.smarthealth.repository.*;
import com.pp.smarthealth.service.ClinicalAIClient;
import com.pp.smarthealth.service.impl.EmailService;
import com.pp.smarthealth.util.JwtUtil;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @Transactional
class VisitIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired PatientRepository patients;
    @Autowired DoctorRepository doctors;
    @Autowired AppointmentRepository appointments;
    @Autowired HealthMetricsRepository metrics;
    @Autowired JwtUtil jwt;
    @MockitoBean ClinicalAIClient ai;
    @MockitoBean ScheduledTasks tasks;
    @MockitoBean EmailService email;
    Patient patient, otherPatient;
    Doctor doctor, otherDoctor;
    String pt, dt, ot;
    @BeforeEach void setup() {
        patient=patient("visit-patient"); otherPatient=patient("visit-other");
        doctor=doctor("visit-doctor"); otherDoctor=doctor("visit-other-doctor");
        pt=token(patient.getUsername()); dt=token(doctor.getUsername()); ot=token(otherDoctor.getUsername());
    }
    private String token(String user) { return "Bearer "+jwt.generateToken(user); }
    private Patient patient(String user) {
        var p=new Patient(); p.setUsername(user); p.setName(user); p.setEmail(user+"@example.test");p.setPassword("unused");p.setAge(30);
        return patients.saveAndFlush(p);
    }
    private Doctor doctor(String user) {
        var d=new Doctor();d.setUsername(user);d.setName(user);d.setEmail(user+"@example.test");d.setPassword("unused");d.setSpecialization("General medicine");
        return doctors.saveAndFlush(d);
    }
    private Appointment pastVisit() {
        var a=new Appointment();a.setPatient(patient);a.setDoctor(doctor);a.setDateTime(LocalDateTime.now().minusHours(2));a.setStatus("Scheduled");a.setNotes("My recent readings changed.");
        return appointments.saveAndFlush(a);
    }
    private HealthMetrics reading(int days, int rate, Map<String,String> units) {
        var m=new HealthMetrics();m.setPatient(patient);m.setDate(java.time.LocalDate.now().minusDays(days));m.setTimestamp(LocalDateTime.now().minusDays(days));m.setHeartRate(rate);m.setUnits(units);
        return metrics.saveAndFlush(m);
    }
    private String approval() { return """
            {"summary":"We discussed your readings. Follow the plan agreed at your visit.",
             "actions":["Bring your readings to your follow-up."],"approve":true,"visitCompleted":true,"followUpDate":"2030-01-02"}
            """; }
    @Test void bookingAutomaticallyStoresBriefAndPatientWordsWithoutCallingAI() throws Exception {
        String body=mvc.perform(post("/api/appointments/book").header("Authorization",pt).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("doctorId",doctor.getId(),"dateTime","2030-01-01T10:00:00","notes","I feel tired.","questions","Can we discuss my sleep?"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var a=appointments.findById(json.readTree(body).get("id").asLong()).orElseThrow();
        assertThat(a.getBriefJson()).contains("I feel tired.","Can we discuss my sleep?","No saved measurements");
        mvc.perform(get("/api/appointments/{id}/brief",a.getId()).header("Authorization",dt)).andExpect(status().isOk())
                .andExpect(jsonPath("$.questions").value("Can we discuss my sleep?"));
        verifyNoInteractions(ai);
    }
    @Test void onlyAssignedDoctorCanReadBriefOrPrepareAndApproveSummary() throws Exception {
        var a=pastVisit();
        mvc.perform(get("/api/appointments/{id}/brief",a.getId())).andExpect(status().isUnauthorized());
        for (String denied : new String[]{pt,ot}) {
            mvc.perform(get("/api/appointments/{id}/brief",a.getId()).header("Authorization",denied)).andExpect(status().isForbidden());
            mvc.perform(post("/api/appointments/{id}/summary/draft",a.getId()).header("Authorization",denied).contentType(MediaType.APPLICATION_JSON).content("{\"doctorNotes\":\"Discussed readings\"}")).andExpect(status().isForbidden());
            mvc.perform(post("/api/appointments/{id}/summary",a.getId()).header("Authorization",denied).contentType(MediaType.APPLICATION_JSON).content(approval())).andExpect(status().isForbidden());
        }
        mvc.perform(get("/api/appointments/visits").header("Authorization",ot)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        verifyNoInteractions(ai);
    }
    @Test void draftIsPrivateAndApprovalIsExplicitImmutableAndPatientScoped() throws Exception {
        var a=pastVisit();
        when(ai.summarizeVisit(anyString(),anyString())).thenReturn("{\"summary\":\"We discussed your readings.\"}");
        mvc.perform(post("/api/appointments/{id}/summary/draft",a.getId()).header("Authorization",dt).contentType(MediaType.APPLICATION_JSON).content("{\"doctorNotes\":\"Discussed readings\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.aiAvailable").value(true));
        mvc.perform(get("/api/appointments/{id}/summary",a.getId()).header("Authorization",pt)).andExpect(status().isOk()).andExpect(jsonPath("$.summary").isEmpty());
        mvc.perform(post("/api/appointments/{id}/summary",a.getId()).header("Authorization",dt).contentType(MediaType.APPLICATION_JSON).content(approval().replace("\"approve\":true","\"approve\":false"))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/appointments/{id}/summary",a.getId()).header("Authorization",dt).contentType(MediaType.APPLICATION_JSON).content(approval())).andExpect(status().isOk());
        mvc.perform(get("/api/appointments/{id}/summary",a.getId()).header("Authorization",pt)).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED")).andExpect(jsonPath("$.summary.doctorId").value(doctor.getId()))
                .andExpect(jsonPath("$.summary.actions[0]").value("Bring your readings to your follow-up."));
        for(String denied: new String[]{ot,token(otherPatient.getUsername())}) mvc.perform(get("/api/appointments/{id}/summary",a.getId()).header("Authorization",denied)).andExpect(status().isForbidden());
        mvc.perform(post("/api/appointments/{id}/summary",a.getId()).header("Authorization",dt).contentType(MediaType.APPLICATION_JSON).content(approval())).andExpect(status().isBadRequest());
        mvc.perform(delete("/api/appointments/{id}",a.getId()).header("Authorization",pt)).andExpect(status().isForbidden());
        assertThat(a.getStatus()).isEqualTo("Completed");
    }
    @Test void providerFailureOrMalformedOutputNeverPublishesAndManualApprovalStillWorks() throws Exception {
        var a=pastVisit();
        when(ai.summarizeVisit(anyString(),anyString())).thenThrow(new IllegalStateException("PRIVATE_PROVIDER_BODY"));
        var result=mvc.perform(post("/api/appointments/{id}/summary/draft",a.getId()).header("Authorization",dt).contentType(MediaType.APPLICATION_JSON).content("{\"doctorNotes\":\"Discussed readings\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.aiAvailable").value(false)).andReturn().getResponse().getContentAsString();
        assertThat(result).doesNotContain("PRIVATE_PROVIDER_BODY");
        doReturn("{\"summary\":42}").when(ai).summarizeVisit(anyString(),anyString());
        mvc.perform(post("/api/appointments/{id}/summary/draft",a.getId()).header("Authorization",dt).contentType(MediaType.APPLICATION_JSON).content("{\"doctorNotes\":\"Discussed readings\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.aiAvailable").value(false));
        assertThat(a.getApprovedSummaryJson()).isNull();
        mvc.perform(post("/api/appointments/{id}/summary",a.getId()).header("Authorization",dt).contentType(MediaType.APPLICATION_JSON).content(approval())).andExpect(status().isOk());
    }
    @Test void trendsUseCodeAndMatchingRecordedUnitsAndBriefRefreshesAfterBooking() throws Exception {
        var a=pastVisit();reading(5,70,Map.of("heartRate","bpm"));reading(1,84,Map.of("heartRate","bpm"));
        mvc.perform(get("/api/appointments/{id}/brief",a.getId()).header("Authorization",dt)).andExpect(status().isOk())
                .andExpect(jsonPath("$.trends[0].absoluteChange").value(14)).andExpect(jsonPath("$.trends[0].percentageChange").value(20));
        reading(0,90,Map.of());
        mvc.perform(get("/api/appointments/{id}/brief",a.getId()).header("Authorization",dt)).andExpect(status().isOk())
                .andExpect(jsonPath("$.trends.length()").value(0)).andExpect(jsonPath("$.latestReports[0].values.heartRate").value(90));
        verifyNoInteractions(ai);
    }
    @Test void partialNewestReportDoesNotHideOtherMeasurementsComparisons() throws Exception {
        var a=pastVisit();
        reading(5,70,Map.of("heartRate","bpm"));
        reading(2,84,Map.of("heartRate","bpm"));
        var weight=reading(1,0,Map.of("weight","kg"));
        weight.setHeartRate(null);weight.setWeight(72.0);metrics.saveAndFlush(weight);
        mvc.perform(get("/api/appointments/{id}/brief",a.getId()).header("Authorization",dt)).andExpect(status().isOk())
                .andExpect(jsonPath("$.trends[0].metric").value("heartRate"))
                .andExpect(jsonPath("$.trends[0].latestValue").value(84))
                .andExpect(jsonPath("$.trends[0].absoluteChange").value(14))
                .andExpect(jsonPath("$.latestReports[0].values.weight").value(72));
    }
    @Test void unitlessSavedValuesAreVisibleWithoutInventingTrends() throws Exception {
        var a=pastVisit();reading(3,70,Map.of());reading(1,84,Map.of());
        mvc.perform(get("/api/appointments/{id}/brief",a.getId()).header("Authorization",dt)).andExpect(status().isOk())
                .andExpect(jsonPath("$.trends.length()").value(0))
                .andExpect(jsonPath("$.latestReports[0].values.heartRate").value(84));
    }
    @Test void excludedFutureReportsAreExplainedInsteadOfClaimingNoSavedData() throws Exception {
        var a=pastVisit();reading(-3,84,Map.of("heartRate","bpm"));
        String result=mvc.perform(get("/api/appointments/{id}/brief",a.getId()).header("Authorization",dt)).andExpect(status().isOk())
                .andExpect(jsonPath("$.latestReports.length()").value(0)).andReturn().getResponse().getContentAsString();
        assertThat(result).contains("future timestamps", "Saved reports were found").doesNotContain("No saved measurements are available");
    }
    @Test void staleDataIsExplicitAndFutureOrCanceledVisitsCannotBePublished() throws Exception {
        var a=pastVisit();reading(40,70,Map.of("heartRate","bpm"));
        var body=mvc.perform(get("/api/appointments/{id}/brief",a.getId()).header("Authorization",dt)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(body).contains("No measurements recorded in the last 30 days");
        a.setDateTime(LocalDateTime.now().plusDays(1));appointments.saveAndFlush(a);
        mvc.perform(post("/api/appointments/{id}/summary",a.getId()).header("Authorization",dt).contentType(MediaType.APPLICATION_JSON).content(approval())).andExpect(status().isBadRequest());
        a.setDateTime(LocalDateTime.now().minusDays(1));a.setStatus("Canceled");appointments.saveAndFlush(a);
        mvc.perform(post("/api/appointments/{id}/summary",a.getId()).header("Authorization",dt).contentType(MediaType.APPLICATION_JSON).content(approval())).andExpect(status().isBadRequest());
    }
    @Test void reassignmentCannotBypassVisitOwnershipAndLegacyAIIsOffByDefault() throws Exception {
        var a=pastVisit();
        var change=json.writeValueAsString(Map.of("doctorId",otherDoctor.getId(),"patientId",patient.getId(),"dateTime","2030-01-01T10:00:00","status","Scheduled"));
        mvc.perform(put("/api/appointments/{id}",a.getId()).header("Authorization",ot).contentType(MediaType.APPLICATION_JSON).content(change)).andExpect(status().isForbidden());
        mvc.perform(put("/api/appointments/{id}",a.getId()).header("Authorization",dt).contentType(MediaType.APPLICATION_JSON).content(change)).andExpect(status().isBadRequest());
        for(String path:new String[]{"/api/ai/symptom-analysis","/api/ai/change-analysis/me","/api/report-imports"}) {
            mvc.perform(post(path).header("Authorization",pt).contentType(MediaType.APPLICATION_JSON).content("{}" )).andExpect(status().isNotFound());
        }
        verifyNoInteractions(ai);
    }
}
