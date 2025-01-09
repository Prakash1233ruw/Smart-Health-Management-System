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
    private Integer bloodGlucoseLevel;
    private Double bmi;
    private Integer totalCholesterol;
    private Integer hdlCholesterol;
    private Integer ldlCholesterol;
    private Integer triglycerides;
    private Double weight;
    private Double height;
    private Double waistCircumference;
    private Integer physicalActivityLevel;
    private Long patientId; 
    private String patientName;
}
