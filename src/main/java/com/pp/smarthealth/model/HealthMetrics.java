package com.pp.smarthealth.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "health_metrics")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HealthMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;
    @Convert(converter = MetricUnitConverter.class)
    @Column(name="units_json", length=4000)
    private java.util.Map<String,String> units = new java.util.LinkedHashMap<>();
    public void setBloodGlucoseLevel(Number value) { this.bloodGlucoseLevel = value == null ? null : value.doubleValue(); }
    public void setTotalCholesterol(Number value) { this.totalCholesterol = value == null ? null : value.doubleValue(); }
    public void setHdlCholesterol(Number value) { this.hdlCholesterol = value == null ? null : value.doubleValue(); }
    public void setLdlCholesterol(Number value) { this.ldlCholesterol = value == null ? null : value.doubleValue(); }
    public void setTriglycerides(Number value) { this.triglycerides = value == null ? null : value.doubleValue(); }
}
