package com.pp.smarthealth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pp.smarthealth.config.ScheduledTasks;
import com.pp.smarthealth.model.*;
import com.pp.smarthealth.repository.*;
import com.pp.smarthealth.service.ClinicalAIClient;
import com.pp.smarthealth.service.impl.EmailService;
import com.pp.smarthealth.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @Transactional
class BookingIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired PatientRepository patients;
    @Autowired DoctorRepository doctors;
    @Autowired AppointmentRepository appointments;
    @Autowired JwtUtil jwt;
    @MockitoBean ClinicalAIClient client;
    @MockitoBean ScheduledTasks scheduledTasks;
    @MockitoBean EmailService emailService;
    Patient patient;
    Doctor doctor;
    String token;

    @BeforeEach void setup() {
        patient = new Patient(); patient.setUsername("booking-patient"); patient.setName("Booking Patient");
        patient.setEmail("booking-patient@example.test"); patient.setPassword("unused"); patient.setAge(30);
        patient = patients.saveAndFlush(patient);
        doctor = new Doctor(); doctor.setUsername("booking-doctor"); doctor.setName("Booking Doctor");
        doctor.setEmail("booking-doctor@example.test"); doctor.setPassword("PRIVATE_HASH"); doctor.setSpecialization("General medicine");
        doctor = doctors.saveAndFlush(doctor);
        token = "Bearer " + jwt.generateToken(patient.getUsername());
    }
    @Test void directoryIsAuthenticatedAndExcludesPrivateAccountFields() throws Exception {
        mvc.perform(get("/api/doctors/directory")).andExpect(status().isUnauthorized());
        var body = mvc.perform(get("/api/doctors/directory").header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].name").value("Booking Doctor"))
                .andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("PRIVATE_HASH", "password", "email", "username");
        mvc.perform(get("/api/doctors").header("Authorization", token)).andExpect(status().isForbidden());
    }
    @Test void bookingUsesPrincipalAndServerStatusAndCancellationRequiresOwnership() throws Exception {
        long id = createBooking();
        var saved = appointments.findById(id).orElseThrow();
        assertThat(saved.getPatient().getId()).isEqualTo(patient.getId());
        assertThat(saved.getStatus()).isEqualTo("Scheduled");
        mvc.perform(get("/api/appointments/myappointments").header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(id));
        var other = new Patient(); other.setName("Other"); other.setUsername("booking-other");
        other.setPassword("unused"); other.setEmail("booking-other@example.test"); other.setAge(30);
        patients.saveAndFlush(other);
        mvc.perform(delete("/api/appointments/{id}", id).header("Authorization","Bearer " + jwt.generateToken(other.getUsername())))
                .andExpect(status().isForbidden());
        assertThat(appointments.findById(id).orElseThrow().getStatus()).isEqualTo("Scheduled");
        mvc.perform(get("/api/doctors/myupcoming-appointments").header("Authorization","Bearer " + jwt.generateToken(doctor.getUsername())))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(id));
        mvc.perform(delete("/api/appointments/{id}", id).header("Authorization",token)).andExpect(status().isOk());
        assertThat(appointments.findById(id).orElseThrow().getStatus()).isEqualTo("Canceled");
    }
    @Test void invalidBookingInputsAndWrongRoleAreRejectedBeforePersistence() throws Exception {
        for (String body : new String[]{
                "{\"doctorId\":1,\"dateTime\":\"2000-01-01T10:00:00\"}",
                "{\"doctorId\":0,\"dateTime\":\"2030-01-01T10:00:00\"}",
                "{\"doctorId\":1}",
                "{\"doctorId\":1,\"dateTime\":\"2030-01-01T10:00:00\",\"notes\":\"" + "x".repeat(501) + "\"}"}) {
            mvc.perform(post("/api/appointments/book").header("Authorization",token)
                    .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        }
        mvc.perform(post("/api/appointments/book").header("Authorization","Bearer " + jwt.generateToken(doctor.getUsername()))
                .contentType(MediaType.APPLICATION_JSON).content(validBody())).andExpect(status().isForbidden());
        mvc.perform(post("/api/appointments/book").header("Authorization",token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"doctorId\":999999,\"dateTime\":\"2030-01-01T10:00:00\"}"))
                .andExpect(status().isNotFound());
        assertThat(appointments.count()).isZero();
    }
    @Test void patientAndDoctorCanRegisterThenLoginWithoutPasswordExposure() throws Exception {
        var patientResult = mvc.perform(post("/api/patients/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"New Patient","username":"new-booking-patient","email":"new-booking-patient@example.test","age":25,"password":"MockPassword123"}
                """)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(patientResult).doesNotContain("password", "MockPassword123");
        assertThat(new BCryptPasswordEncoder().matches("MockPassword123", patients.findByUsername("new-booking-patient").orElseThrow().getPassword())).isTrue();
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"new-booking-patient\",\"password\":\"MockPassword123\"}"))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.startsWith("JWT Token: ")));
        var doctorResult = mvc.perform(post("/api/doctors/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"New Doctor","username":"new-booking-doctor","email":"new-booking-doctor@example.test","specialization":"General medicine","password":"MockPassword123"}
                """)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(doctorResult).doesNotContain("password", "MockPassword123");
        assertThat(new BCryptPasswordEncoder().matches("MockPassword123", doctors.findByUsername("new-booking-doctor").orElseThrow().getPassword())).isTrue();
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"new-booking-doctor\",\"password\":\"MockPassword123\"}"))
                .andExpect(status().isOk());
    }
    @Test void confirmationEmailFailureDoesNotTurnSavedBookingIntoAnError() throws Exception {
        org.mockito.Mockito.doThrow(new org.springframework.mail.MailSendException("PRIVATE SMTP DETAIL"))
                .when(emailService).sendEmailWithTemplate(org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyMap());
        long id = createBooking();
        assertThat(appointments.findById(id)).isPresent();
        assertThat(appointments.count()).isEqualTo(1);
    }
    private String validBody() {
        return "{\"doctorId\":" + doctor.getId() + ",\"dateTime\":\"2030-01-01T10:00:00\",\"notes\":\"Demo consultation\",\"patientId\":999999,\"status\":\"Reviewed\"}";
    }
    private long createBooking() throws Exception {
        var body = mvc.perform(post("/api/appointments/book").header("Authorization",token)
                .contentType(MediaType.APPLICATION_JSON).content(validBody())).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("id").asLong();
    }
}
