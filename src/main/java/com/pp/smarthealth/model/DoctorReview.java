package com.pp.smarthealth.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import com.pp.smarthealth.dto.HealthTimelineDtos.DoctorReviewRequest;
import com.pp.smarthealth.model.HealthTimelineTypes.ReviewStatus;

/** Reviews are immutable audit events; subsequent permitted reviews append a new row. */
@Entity
@Table(name = "doctor_reviews", indexes =
        @Index(name = "idx_review_analysis_time", columnList = "analysis_id,reviewed_at,id"))
@Getter @NoArgsConstructor
public class DoctorReview {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "analysis_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private HealthChangeAnalysis analysis;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Doctor doctor;
    @Column(nullable = false, updatable = false)
    private Long reviewerDoctorId;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 24)
    private ReviewStatus status;
    @Column(length = 3000)
    private String internalNotes;
    @Column(length = 1000)
    private String followUpRecommendation;
    private LocalDate recommendedFollowUpDate;
    @Column(nullable = false, updatable = false)
    private LocalDateTime reviewedAt;
    public DoctorReview(HealthChangeAnalysis analysis, Doctor doctor, DoctorReviewRequest request) {
        this.analysis = analysis;
        this.doctor = doctor;
        this.reviewerDoctorId = doctor.getId();
        this.status = request.status();
        this.internalNotes = request.internalNotes();
        this.followUpRecommendation = request.followUpRecommendation();
        this.recommendedFollowUpDate = request.recommendedFollowUpDate();
    }
    @PrePersist
    void created() { reviewedAt = LocalDateTime.now(); }
}
