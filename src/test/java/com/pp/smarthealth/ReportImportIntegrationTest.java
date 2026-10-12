package com.pp.smarthealth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pp.smarthealth.config.ScheduledTasks;
import com.pp.smarthealth.model.*;
import com.pp.smarthealth.repository.*;
import com.pp.smarthealth.service.*;
import com.pp.smarthealth.service.impl.EmailService;
import com.pp.smarthealth.util.JwtUtil;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@org.springframework.test.context.TestPropertySource(properties="app.legacy-ai-enabled=true")
@SpringBootTest @AutoConfigureMockMvc @Transactional
class ReportImportIntegrationTest {
    @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired JwtUtil jwt;
    @Autowired PatientRepository patients; @Autowired DoctorRepository doctors;
    @Autowired HealthMetricsRepository metrics; @Autowired ReportImportDraftRepository drafts;
    @MockitoBean ReportExtractionClient extractor; @MockitoBean ClinicalAIClient clinical;
    @MockitoBean ScheduledTasks scheduled; @MockitoBean EmailService mail;
    Patient patient; Doctor doctor; String token;
    @BeforeEach void setup() {
        patient=new Patient();patient.setUsername("report-patient");patient.setName("Fictional Patient");patient.setPassword("unused");patient.setEmail("report-patient@example.test");patient.setAge(30);patient=patients.saveAndFlush(patient);
        doctor=new Doctor();doctor.setUsername("report-doctor");doctor.setName("Fictional Doctor");doctor.setPassword("unused");doctor.setEmail("report-doctor@example.test");doctor.setSpecialization("General");doctor=doctors.saveAndFlush(doctor);
        token="Bearer "+jwt.generateToken(doctor.getUsername());
        when(extractor.extract(any())).thenReturn("""
            {"reportDate":"2026-01-01","rows":[{"metric":"bloodGlucoseLevel","value":"5.2","unit":"mmol/L","sourceText":"Glucose 5.2 mmol/L","page":1}],"warnings":[]}
            """);
    }
    @Test void uploadCreatesOnlyDraftAndConfirmationStoresDecimalsUnitsAndDateExactlyOnce() throws Exception {
        long id=upload();assertThat(metrics.count()).isZero();
        mvc.perform(get("/api/report-imports/{id}",id).header("Authorization",token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.rows[0].evidenceStatus").value("TEXT_MATCHED"));
        String body=confirmation("bloodGlucoseLevel","5.2","mmol/L",true);
        for(int attempt=0;attempt<2;attempt++) mvc.perform(post("/api/report-imports/{id}/confirm",id).header("Authorization",token).contentType("application/json").content(body))
            .andExpect(status().isOk()).andExpect(jsonPath("$.bloodGlucoseLevel").value(5.2)).andExpect(jsonPath("$.units.bloodGlucoseLevel").value("mmol/L"));
        assertThat(metrics.count()).isEqualTo(1);var saved=metrics.findAll().get(0);assertThat(saved.getDate()).isEqualTo(LocalDate.of(2026,1,1));assertThat(saved.getPatient().getId()).isEqualTo(patient.getId());
        assertThat(drafts.findById(id).orElseThrow().getConfirmedRequestJson()).contains("5.2");
    }
    @Test void rejectsUnverifiedUnsupportedDuplicateAndFutureDateConfirmationsWithoutSaving() throws Exception {
        long id=upload();
        for(String body:java.util.List.of(confirmation("bloodGlucoseLevel","5.2","mmol/L",false),confirmation("bloodGlucoseLevel","5.2","mystery",true),confirmation("unknownMetric","5.2","mmol/L",true),confirmation("heartRate","5.2","bpm",true)))
            mvc.perform(post("/api/report-imports/{id}/confirm",id).header("Authorization",token).contentType("application/json").content(body)).andExpect(status().isBadRequest());
        String body=confirmation("bloodGlucoseLevel","5.2","mmol/L",true).replace("2026-01-01",LocalDate.now().plusDays(1).toString());
        mvc.perform(post("/api/report-imports/{id}/confirm",id).header("Authorization",token).contentType("application/json").content(body)).andExpect(status().isBadRequest());
        assertThat(metrics.count()).isZero();
    }
    @Test void patientAndOtherDoctorsCannotReadOrConfirmDrafts() throws Exception {
        String patientToken="Bearer "+jwt.generateToken(patient.getUsername());
        mvc.perform(multipart("/api/report-imports").file(pdf()).param("patientId",patient.getId().toString()).header("Authorization",patientToken)).andExpect(status().isForbidden());
        verifyNoInteractions(extractor);long id=upload();
        var other=new Doctor();other.setUsername("report-other");other.setName("Other");other.setPassword("unused");other.setEmail("report-other@example.test");other.setSpecialization("General");doctors.saveAndFlush(other);
        String otherToken="Bearer "+jwt.generateToken(other.getUsername());
        mvc.perform(get("/api/report-imports/{id}",id).header("Authorization",otherToken)).andExpect(status().isForbidden());
        mvc.perform(post("/api/report-imports/{id}/confirm",id).header("Authorization",otherToken).contentType("application/json").content(confirmation("bloodGlucoseLevel","5.2","mmol/L",true))).andExpect(status().isForbidden());
        assertThat(metrics.count()).isZero();
    }
    @Test void malformedFilesAndTooManyPagesNeverReachGemini() throws Exception {
        mvc.perform(multipart("/api/report-imports").file(new MockMultipartFile("file","pretend.pdf","application/pdf","not a PDF".getBytes())).param("patientId",patient.getId().toString()).header("Authorization",token)).andExpect(status().isBadRequest());
        try(var pdf=new PDDocument();var out=new ByteArrayOutputStream()) {
            for(int i=0;i<6;i++)pdf.addPage(new PDPage());pdf.save(out);
            mvc.perform(multipart("/api/report-imports").file(new MockMultipartFile("file","six.pdf","application/pdf",out.toByteArray())).param("patientId",patient.getId().toString()).header("Authorization",token)).andExpect(status().isBadRequest());
        }
        verifyNoInteractions(extractor);assertThat(drafts.count()).isZero();
    }
    @Test void missingDateAndUnverifiableExcerptRemainFlaggedRatherThanInvented() throws Exception {
        when(extractor.extract(any())).thenReturn("""
            {"reportDate":null,"rows":[{"metric":"bloodGlucoseLevel","value":"100","unit":"","sourceText":"invented excerpt","page":1}],"warnings":[]}
            """);
        long id=upload();mvc.perform(get("/api/report-imports/{id}",id).header("Authorization",token)).andExpect(status().isOk()).andExpect(jsonPath("$.reportDate").isEmpty()).andExpect(jsonPath("$.rows[0].evidenceStatus").value("UNVERIFIED"));
        assertThat(metrics.count()).isZero();
    }
    @Test void providerFailuresDoNotCreateDraftOrMeasurement() throws Exception {
        when(extractor.extract(any())).thenThrow(com.pp.smarthealth.exception.AIServiceException.unavailable());
        mvc.perform(multipart("/api/report-imports").file(pdf()).param("patientId",patient.getId().toString()).header("Authorization",token)).andExpect(status().isServiceUnavailable());
        assertThat(drafts.count()).isZero();assertThat(metrics.count()).isZero();
    }
    @Test void verifiedUnitsEnableComparisonWithoutChangingValuesOrAddingRecords() throws Exception {
        var earlier=new HealthMetrics();earlier.setPatient(patient);earlier.setDate(LocalDate.of(2026,1,1));earlier.setTimestamp(java.time.LocalDateTime.of(2026,1,1,10,0));earlier.setHeartRate(72);earlier.setUnits(java.util.Map.of("heartRate","bpm"));metrics.saveAndFlush(earlier);
        var latest=new HealthMetrics();latest.setPatient(patient);latest.setDate(LocalDate.of(2026,1,2));latest.setTimestamp(java.time.LocalDateTime.of(2026,1,2,10,0));latest.setHeartRate(84);latest=metrics.saveAndFlush(latest);
        var calculator=new HealthChangeCalculator();
        assertThat(calculator.metrics(java.util.List.of(latest,earlier)).stream().filter(m->m.metric().equals("heartRate")).findFirst().orElseThrow().absoluteChange()).isNull();
        mvc.perform(patch("/api/healthmetrics/{id}/units",latest.getId()).header("Authorization",token).contentType("application/json").content("""
            {"units":{"heartRate":"bpm"},"verifiedAgainstSource":true}
            """)).andExpect(status().isOk()).andExpect(jsonPath("$.heartRate").value(84)).andExpect(jsonPath("$.units.heartRate").value("bpm"));
        assertThat(metrics.count()).isEqualTo(2);
        var updated=metrics.findById(latest.getId()).orElseThrow();
        assertThat(updated.getTimestamp()).isEqualTo(latest.getTimestamp());
        assertThat(calculator.metrics(java.util.List.of(updated,earlier)).stream().filter(m->m.metric().equals("heartRate")).findFirst().orElseThrow().absoluteChange()).isEqualByComparingTo("12");
    }
    @Test void unitAdditionRequiresDoctorSourceVerificationAndValidRecordedMetric() throws Exception {
        var record=new HealthMetrics();record.setPatient(patient);record.setDate(LocalDate.of(2026,1,2));record.setTimestamp(java.time.LocalDateTime.of(2026,1,2,10,0));record.setHeartRate(84);record.setTemperature(36.8);record.setUnits(java.util.Map.of("temperature","°C"));record=metrics.saveAndFlush(record);
        String url="/api/healthmetrics/"+record.getId()+"/units";
        mvc.perform(patch(url).header("Authorization","Bearer "+jwt.generateToken(patient.getUsername())).contentType("application/json").content("{\"units\":{\"heartRate\":\"bpm\"},\"verifiedAgainstSource\":true}")).andExpect(status().isForbidden());
        for(String body:java.util.List.of(
            "{\"units\":{\"heartRate\":\"bpm\"},\"verifiedAgainstSource\":false}",
            "{\"units\":{\"heartRate\":\"kg\"},\"verifiedAgainstSource\":true}",
            "{\"units\":{\"weight\":\"kg\"},\"verifiedAgainstSource\":true}",
            "{\"units\":{\"temperature\":\"°F\"},\"verifiedAgainstSource\":true}"))
            mvc.perform(patch(url).header("Authorization",token).contentType("application/json").content(body)).andExpect(status().isBadRequest());
        assertThat(metrics.findById(record.getId()).orElseThrow().getUnits()).containsExactlyEntriesOf(java.util.Map.of("temperature","°C"));
    }
    private long upload() throws Exception {
        var response=mvc.perform(multipart("/api/report-imports").file(pdf()).param("patientId",patient.getId().toString()).header("Authorization",token)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(response).path("draftId").asLong();
    }
    private MockMultipartFile pdf() throws Exception {
        try(var pdf=new PDDocument();var out=new ByteArrayOutputStream()) {
            var page=new PDPage();pdf.addPage(page);
            try(var content=new PDPageContentStream(pdf,page)) { content.beginText();content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA),12);content.newLineAtOffset(40,700);content.showText("Fictional report: Glucose 5.2 mmol/L");content.endText(); }
            pdf.save(out);return new MockMultipartFile("file","fictional-report.pdf","application/pdf",out.toByteArray());
        }
    }
    private String confirmation(String metric,String value,String unit,boolean verified) {
        return "{\"recordedDate\":\"2026-01-01\",\"verifiedAgainstReport\":"+verified+",\"rows\":[{\"index\":0,\"metric\":\""+metric+"\",\"value\":"+value+",\"unit\":\""+unit+"\"}]}";
    }
}
