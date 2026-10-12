package com.pp.smarthealth.service.impl;

import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.List;
import java.util.StringJoiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import com.pp.smarthealth.exception.AIServiceException;
import com.pp.smarthealth.service.ClinicalAIClient;

@Component
public class SpringAIClinicalClient implements ClinicalAIClient {
    private static final Logger log = LoggerFactory.getLogger(SpringAIClinicalClient.class);
    private final ObjectProvider<OpenAiChatModel> model;

    public SpringAIClinicalClient(ObjectProvider<OpenAiChatModel> model) {
        this.model = model;
    }

    private static final String SCHEMA = """
            {"type":"object","additionalProperties":false,
             "properties":{
               "riskLevel":{"type":"string","enum":["LOW","MODERATE","HIGH","EMERGENCY"]},
               "possibleConditions":{"type":"array","items":{"type":"string"}},
               "recommendedTests":{"type":"array","items":{"type":"string"}},
               "recommendedSpecialist":{"type":"string"},
               "warningSigns":{"type":"array","items":{"type":"string"}},
               "explanation":{"type":"string"}},
             "required":["riskLevel","possibleConditions","recommendedTests",
                         "recommendedSpecialist","warningSigns","explanation"]}
            """;

    @Override
    public String analyze(String systemPrompt, String patientContext) {
        return complete(systemPrompt, patientContext, SCHEMA, "symptom_analysis");
    }

    private static final String CHANGE_SCHEMA = """
            {"type":"object","additionalProperties":false,"properties":{
              "summary":{"type":"string"},
              "importantChanges":{"type":"array","items":{"type":"string"}},
              "itemsForDoctorReview":{"type":"array","items":{"type":"string"}},
              "questionsForDoctor":{"type":"array","items":{"type":"string"}}},
             "required":["summary","importantChanges","itemsForDoctorReview","questionsForDoctor"]}
            """;

    @Override
    public String summarizeChanges(String systemPrompt, String verifiedContext) {
        return complete(systemPrompt, verifiedContext, CHANGE_SCHEMA, "health_changes");
    }

    @Override
    public String summarizeVisit(String systemPrompt, String doctorNotes) {
        return complete(systemPrompt, doctorNotes, """
                {"type":"object","additionalProperties":false,"properties":{
                  "summary":{"type":"string"}},"required":["summary"]}
                """, "visit_summary");
    }

    private String complete(String systemPrompt, String patientContext, String schema, String schemaName) {
        try {
            var format = ResponseFormat.builder().type(ResponseFormat.Type.JSON_SCHEMA)
                    .jsonSchema(ResponseFormat.JsonSchema.builder().name(schemaName)
                            .schema(schema).strict(true).build()).build();
            var options = OpenAiChatOptions.builder().responseFormat(format).build();
            var response = model.getObject().call(new Prompt(List.of(
                    new SystemMessage(systemPrompt), new UserMessage(patientContext)), options));
            if (response == null || response.getResult() == null
                    || response.getResult().getOutput() == null
                    || !"STOP".equalsIgnoreCase(response.getResult().getMetadata().getFinishReason())) {
                throw AIServiceException.invalidResponse();
            }
            String content = response.getResult().getOutput().getText();
            if (content == null || content.isBlank()) {
                throw AIServiceException.invalidResponse();
            }
            return content;
        } catch (AIServiceException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            // Never propagate or log provider bodies, prompts, or credentials.
            StringJoiner failureTypes = new StringJoiner(" -> ");
            for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
                failureTypes.add(cause.getClass().getSimpleName());
                // Lazy bean creation wraps missing configuration in a Spring exception.
                if (cause instanceof AIServiceException safeError) { throw safeError; }
                if (cause instanceof RestClientResponseException httpError) {
                    int status = httpError.getStatusCode().value();
                    log.warn("Gemini request failed: HTTP {} (provider body omitted)", status);
                    if (status == 429) { throw AIServiceException.quotaExceeded(); }
                    if (status == 400 || status == 401 || status == 403 || status == 404) {
                        throw AIServiceException.configurationRejected();
                    }
                }
                if (cause instanceof SocketTimeoutException || cause instanceof HttpTimeoutException) {
                    throw AIServiceException.timeout();
                }
            }
            log.warn("Gemini request failed: exception types={} (details omitted)", failureTypes);
            throw AIServiceException.unavailable();
        }
    }
}
