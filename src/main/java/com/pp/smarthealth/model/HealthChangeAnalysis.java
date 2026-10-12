package com.pp.smarthealth.model;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import com.pp.smarthealth.model.HealthTimelineTypes.ReviewStatus;

@Entity
@Table(name = "health_change_analyses", indexes =
        @Index(name = "idx_change_patient_time", columnList = "patient_id,created_at,id"))
@Getter @Setter @NoArgsConstructor
public class HealthChangeAnalysis {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Patient patient;
    @Lob @Column(columnDefinition = "TEXT")
    private String symptoms;
    @Lob @Column(nullable = false, columnDefinition = "MEDIUMTEXT")
    private String responseJson;
    @Column(nullable = false, length = 100)
    private String modelName;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 24)
    private ReviewStatus reviewStatus = ReviewStatus.PENDING_REVIEW;
    @Version
    private long version;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @PrePersist
    void created() { createdAt = LocalDateTime.now(); }
}
