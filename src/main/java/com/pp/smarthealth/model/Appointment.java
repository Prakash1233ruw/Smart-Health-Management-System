package com.pp.smarthealth.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "appointments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date_time", nullable = false)
    private LocalDateTime dateTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;
    private String status;
    
    @Column(name = "notes", length = 500)
    private String notes;

    @Column(length = 1000)
    private String questions;
    @Lob @Column(columnDefinition = "MEDIUMTEXT")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String briefJson;
    @Lob @Column(columnDefinition = "TEXT")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String approvedSummaryJson;
    @com.fasterxml.jackson.annotation.JsonIgnore
    private LocalDateTime summaryApprovedAt;
}
