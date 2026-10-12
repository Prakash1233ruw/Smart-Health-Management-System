package com.pp.smarthealth.config;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@Getter
@Setter
@ConfigurationProperties(prefix = "health-timeline")
public class HealthTimelineProperties {
    private boolean enabled = true;
    @Min(2) @Max(20)
    private int metricsHistoryLimit = 5;
    @Min(1) @Max(100)
    private int maxPageSize = 100;
}
