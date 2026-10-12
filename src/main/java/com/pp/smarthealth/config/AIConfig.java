package com.pp.smarthealth.config;

import java.time.Duration;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.DefaultResponseErrorHandler;
import com.pp.smarthealth.exception.AIServiceException;

@Configuration
@PropertySource("classpath:ai.properties")
@EnableConfigurationProperties(AIProperties.class)
public class AIConfig {
    // Lazy construction lets the existing application run without an AI key.
    @Bean
    @Lazy
    public OpenAiChatModel clinicalChatModel(AIProperties properties) {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw AIServiceException.missingKey();
        }
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(properties.getConnectTimeoutSeconds()));
        factory.setReadTimeout(Duration.ofSeconds(properties.getReadTimeoutSeconds()));
        return createModel(properties, RestClient.builder().requestFactory(factory));
    }

    // Gemini implements the OpenAI wire protocol, so Spring AI 1.0 can be reused.
    // The host and path are fixed to Google; credentials are never sent to OpenAI.
    OpenAiChatModel createModel(AIProperties properties, RestClient.Builder restClientBuilder) {
        OpenAiApi api = OpenAiApi.builder()
                .baseUrl("https://generativelanguage.googleapis.com")
                .completionsPath("/v1beta/openai/chat/completions")
                .apiKey(properties.getApiKey())
                .restClientBuilder(restClientBuilder)
                .responseErrorHandler(new DefaultResponseErrorHandler())
                .build();
        return OpenAiChatModel.builder().openAiApi(api)
                .defaultOptions(OpenAiChatOptions.builder().model(properties.getModel())
                        .temperature(0.0).maxTokens(1800).build())
                .retryTemplate(RetryTemplate.builder().maxAttempts(1).build()).build();
    }
}
