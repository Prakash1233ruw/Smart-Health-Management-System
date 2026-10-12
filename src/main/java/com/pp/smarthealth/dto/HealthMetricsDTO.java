package com.pp.smarthealth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HealthMetricsDTO {
    private Long id;
    private LocalDate date;
    private LocalDateTime timestamp;
    private Integer systolicPressure;
    private Integer diastolicPressure;
    private Integer heartRate;
    private Double temperature;
    private Integer respiratoryRate;
    private Integer bloodOxygenLevel;
    private Double bloodGlucoseLevel;
    private Double bmi;
    private Double totalCholesterol;
    private Double hdlCholesterol;
    private Double ldlCholesterol;
    private Double triglycerides;
    private Double weight;
    private Double height;
    private Double waistCircumference;
    private Integer physicalActivityLevel;
    private Long patientId; 
    private String patientName;
    private java.util.Map<String,String> units = new java.util.LinkedHashMap<>();
    public void setBloodGlucoseLevel(Number value) { this.bloodGlucoseLevel = value == null ? null : value.doubleValue(); }
    public void setTotalCholesterol(Number value) { this.totalCholesterol = value == null ? null : value.doubleValue(); }
    public void setHdlCholesterol(Number value) { this.hdlCholesterol = value == null ? null : value.doubleValue(); }
    public void setLdlCholesterol(Number value) { this.ldlCholesterol = value == null ? null : value.doubleValue(); }
    public void setTriglycerides(Number value) { this.triglycerides = value == null ? null : value.doubleValue(); }
}
