package com.pp.smarthealth.service;

import java.net.SocketTimeoutException;
import java.util.List;
import com.pp.smarthealth.exception.AIServiceException;
import com.pp.smarthealth.service.impl.SpringAIClinicalClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.*;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.client.ResourceAccessException;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, org.springframework.boot.test.system.OutputCaptureExtension.class})
class SpringAIClinicalClientTest {
    @Mock ObjectProvider<OpenAiChatModel> provider;
    @Mock OpenAiChatModel model;
    SpringAIClinicalClient client;
    @BeforeEach void setup() { client = new SpringAIClinicalClient(provider); }

    @Test void sendsSeparateMessagesAndStrictJsonSchema() {
        when(provider.getObject()).thenReturn(model);
        when(model.call(any(Prompt.class))).thenReturn(response("{\"riskLevel\":\"HIGH\"}", "STOP"));
        assertThat(client.analyze("safety instructions", "clinical JSON")).contains("HIGH");
        ArgumentCaptor<Prompt> prompt = ArgumentCaptor.forClass(Prompt.class);
        verify(model).call(prompt.capture());
        assertThat(prompt.getValue().getInstructions()).hasSize(2);
        assertThat(prompt.getValue().getInstructions().get(0).getText()).isEqualTo("safety instructions");
        OpenAiChatOptions options = (OpenAiChatOptions) prompt.getValue().getOptions();
        assertThat(options.getResponseFormat().getType()).isEqualTo(ResponseFormat.Type.JSON_SCHEMA);
        assertThat(options.getResponseFormat().getJsonSchema().getStrict()).isTrue();
    }

    @Test void timeoutReturnsSafe504() {
        when(provider.getObject()).thenReturn(model);
        when(model.call(any(Prompt.class))).thenThrow(new ResourceAccessException("private endpoint",
                new SocketTimeoutException("secret")));
        assertThatThrownBy(() -> client.analyze("", ""))
                .isInstanceOfSatisfying(AIServiceException.class,
                        e -> assertThat(e.getStatus().value()).isEqualTo(504))
                .hasMessage("AI analysis timed out. Please try again later.");
    }

    @Test void providerFailuresReturnSafe503(org.springframework.boot.test.system.CapturedOutput output) {
        when(provider.getObject()).thenThrow(new IllegalStateException("private API key"));
        assertThatThrownBy(() -> client.analyze("", ""))
                .isInstanceOfSatisfying(AIServiceException.class,
                        e -> assertThat(e.getStatus().value()).isEqualTo(503))
                .hasMessage("AI analysis is currently unavailable.");
        assertThat(output.getOut()).contains("exception types=IllegalStateException")
                .doesNotContain("private API key");
    }

    @Test void lazyBeanFailurePreservesMissingKeyMessage() {
        when(provider.getObject()).thenThrow(new org.springframework.beans.factory.BeanCreationException(
                "clinicalChatModel", "internal details", AIServiceException.missingKey()));
        assertThatThrownBy(() -> client.analyze("", ""))
                .isInstanceOf(AIServiceException.class)
                .hasMessage("Gemini API key is not configured. Set AI_API_KEY in the application's run environment and restart.");
    }

    @Test void truncatedAndRefusedResponsesAreRejected() {
        when(provider.getObject()).thenReturn(model);
        for (ChatResponse response : List.of(response("{}", "LENGTH"), response("", "STOP"),
                response("refused", "CONTENT_FILTER"), new ChatResponse(List.of()))) {
            when(model.call(any(Prompt.class))).thenReturn(response);
            assertThatThrownBy(() -> client.analyze("", ""))
                    .isInstanceOfSatisfying(AIServiceException.class,
                            e -> assertThat(e.getStatus().value()).isEqualTo(502));
        }
    }

    private ChatResponse response(String text, String finishReason) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text),
                ChatGenerationMetadata.builder().finishReason(finishReason).build())));
    }
}
