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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;
}
