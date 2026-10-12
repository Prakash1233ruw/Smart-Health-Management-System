package com.pp.smarthealth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pp.smarthealth.dto.SymptomAnalysisResponse;
import com.pp.smarthealth.exception.AIServiceException;
import com.pp.smarthealth.exception.ResourceNotFoundException;
import com.pp.smarthealth.model.Patient;
import com.pp.smarthealth.security.*;
import com.pp.smarthealth.service.AIClinicalService;
import com.pp.smarthealth.service.impl.CustomUserDetailsService;
import com.pp.smarthealth.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@org.springframework.test.context.TestPropertySource(properties="app.legacy-ai-enabled=true")
@WebMvcTest(AIClinicalController.class)
@Import({SecurityConfig.class, JwtRequestFilter.class, JwtUtil.class})
class AIClinicalControllerTest {
    static final String URL = "/api/ai/symptom-analysis";
    static final String BODY = "{\"patientId\":1,\"symptoms\":[\"fever\",\"cough\"]}";
    @Autowired MockMvc mvc;
    @Autowired JwtUtil jwt;
    @Autowired ObjectMapper mapper;
    @MockitoBean AIClinicalService service;
    @MockitoBean CustomUserDetailsService users;

    @Test void validJwtUsesExistingAuthenticationAndReturnsStructuredResponse() throws Exception {
        Patient patient = new Patient();
        patient.setUsername("patient-one");
        when(users.loadUserByUsername("patient-one")).thenReturn(new PatientDetails(patient));
        SymptomAnalysisResponse response = mapper.readValue("""
                {"riskLevel":"HIGH","possibleConditions":["Possible infection"],
                 "recommendedTests":[],"recommendedSpecialist":"Primary care clinician",
                 "warningSigns":[],"explanation":"Professional evaluation is needed."}
                """, SymptomAnalysisResponse.class);
        when(service.analyze(any(), any())).thenReturn(response);
        mvc.perform(post(URL).header("Authorization", "Bearer " + jwt.generateToken("patient-one"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk()).andExpect(jsonPath("$.riskLevel").value("HIGH"))
                .andExpect(jsonPath("$.disclaimer").value(SymptomAnalysisResponse.DISCLAIMER));
        verify(service).analyze(argThat(r -> r.getPatientId().equals(1L)),
                argThat(a -> a.getName().equals("patient-one")));
    }

    @Test void anonymousRequestIsUnauthorized() throws Exception {
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test void malformedJwtIsUnauthorized() throws Exception {
        mvc.perform(post(URL).header("Authorization", "Bearer invalid.token")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test void jwtWithDifferentSigningKeyIsUnauthorized() throws Exception {
        mvc.perform(post(URL).header("Authorization", "Bearer " + new JwtUtil().generateToken("patient-one"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test void adminIsForbidden() throws Exception {
        mvc.perform(post(URL).with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "null", "not-json", "{\"patientId\":0,\"symptoms\":[\"fever\"]}",
            "{\"patientId\":1,\"symptoms\":[]}", "{\"patientId\":1,\"symptoms\":[\" \" ]}",
            "{\"patientId\":1,\"symptoms\":[null]}"})
    void invalidBodyIsBadRequest(String body) throws Exception {
        mvc.perform(post(URL).with(user("patient-one").roles("PATIENT"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test void serviceErrorsHaveExpectedStatusAndSafeMessages() throws Exception {
        RuntimeException[] errors = {new ResourceNotFoundException("Patient not found"),
                new AccessDeniedException("Patient access denied."), AIServiceException.unavailable(),
                AIServiceException.invalidResponse(), AIServiceException.timeout()};
        int[] statuses = {404, 403, 503, 502, 504};
        for (int i = 0; i < errors.length; i++) {
            doThrow(errors[i]).when(service).analyze(any(), any());
            mvc.perform(post(URL).with(user("doctor").roles("DOCTOR"))
                            .contentType(MediaType.APPLICATION_JSON).content(BODY))
                    .andExpect(status().is(statuses[i]))
                    .andExpect(jsonPath("$.message").value(errors[i].getMessage()));
        }
    }
}
