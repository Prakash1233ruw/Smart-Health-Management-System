package com.pp.smarthealth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "ai")
public class AIProperties {
    private String apiKey = "";
    @NotBlank
    private String model = "gemini-2.5-flash-lite";
    @Min(1) @Max(60)
    private int connectTimeoutSeconds = 5;
    @Min(1) @Max(120)
    private int readTimeoutSeconds = 30;
    private boolean saveHistory = false;
}
