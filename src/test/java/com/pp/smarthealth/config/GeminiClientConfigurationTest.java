package com.pp.smarthealth.config;

import com.pp.smarthealth.exception.AIServiceException;
import com.pp.smarthealth.service.impl.SpringAIClinicalClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class GeminiClientConfigurationTest {
    private static final String ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions";

    @Test
    void actualSpringAIRequestUsesGoogleEndpointGeminiKeyModelAndSchema() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        SpringAIClinicalClient client = client(builder);
        server.expect(requestTo(ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer fake-gemini-test-key"))
                .andExpect(jsonPath("$.model").value("gemini-2.5-flash-lite"))
                .andExpect(jsonPath("$.messages[0].role").value("system"))
                .andExpect(jsonPath("$.messages[1].role").value("user"))
                .andExpect(jsonPath("$.response_format.type").value("json_schema"))
                .andExpect(jsonPath("$.response_format.json_schema.strict").value(true))
                .andExpect(jsonPath("$.response_format.json_schema.schema.properties.riskLevel.type").value("string"))
                .andRespond(withSuccess("""
                        {"id":"gemini-test","object":"chat.completion","created":1,
                         "model":"gemini-2.5-flash-lite",
                         "choices":[{"index":0,"finish_reason":"stop",
                           "message":{"role":"assistant","content":"{}"}}],
                         "usage":{"prompt_tokens":10,"completion_tokens":5,"total_tokens":15}}
                        """, MediaType.APPLICATION_JSON));

        assertThat(client.analyze("Safety instructions", "Synthetic clinical context"))
                .isEqualTo("{}");
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 403, 404, 429, 500})
    void googleFailuresAreSanitizedWithoutRetries(int status) {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        SpringAIClinicalClient client = client(builder);
        server.expect(requestTo(ENDPOINT)).andRespond(withStatus(HttpStatusCode.valueOf(status))
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\":{\"message\":\"sensitive provider detail\"}}"));
        assertThatThrownBy(() -> client.analyze("", ""))
                .isInstanceOfSatisfying(AIServiceException.class, ex -> {
                    assertThat(ex.getStatus().value()).isEqualTo(503);
                    assertThat(ex.getMessage()).doesNotContain("sensitive", "fake-gemini-test-key");
                    if (status == 429) {
                        assertThat(ex.getMessage()).contains("usage limit");
                    } else if (status < 500) {
                        assertThat(ex.getMessage()).contains("Gemini rejected");
                    }
                });
        server.verify();
    }

    @Test
    void absentKeyFailsBeforeAnyNetworkCall() {
        assertThatThrownBy(() -> new AIConfig().clinicalChatModel(new AIProperties()))
                .isInstanceOf(AIServiceException.class)
                .hasMessageContaining("Gemini API key is not configured");
    }

    @Test void changeNarrativeUsesSameProviderWithSeparateStrictSchema() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        SpringAIClinicalClient client = client(builder);
        server.expect(requestTo(ENDPOINT))
                .andExpect(header("Authorization", "Bearer fake-gemini-test-key"))
                .andExpect(jsonPath("$.response_format.json_schema.name").value("health_changes"))
                .andExpect(jsonPath("$.response_format.json_schema.strict").value(true))
                .andExpect(jsonPath("$.response_format.json_schema.schema.properties.summary.type").value("string"))
                .andExpect(jsonPath("$.response_format.json_schema.schema.properties.riskLevel").doesNotExist())
                .andExpect(jsonPath("$.response_format.json_schema.schema.properties.disclaimer").doesNotExist())
                .andRespond(withSuccess("""
                        {"id":"test","object":"chat.completion","created":1,
                        "choices":[{"index":0,"finish_reason":"stop","message":{"role":"assistant","content":"{}"}}]}
                        """, MediaType.APPLICATION_JSON));
        assertThat(client.summarizeChanges("Cautious instructions", "Verified changes")).isEqualTo("{}");
        server.verify();
    }

    private SpringAIClinicalClient client(RestClient.Builder builder) {
        AIProperties properties = new AIProperties();
        properties.setApiKey("fake-gemini-test-key");
        OpenAiChatModel model = new AIConfig().createModel(properties, builder);
        // Use the full Spring bean factory, as in production, for ObjectProvider.getObject().
        DefaultListableBeanFactory context = new DefaultListableBeanFactory();
        context.registerSingleton("clinicalChatModel", model);
        return new SpringAIClinicalClient(context.getBeanProvider(OpenAiChatModel.class));
    }
}
