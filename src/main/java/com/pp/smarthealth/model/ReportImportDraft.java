package com.pp.smarthealth.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity @Table(name="report_import_drafts") @Data
public class ReportImportDraft {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Version private long version;
    @Column(nullable=false) private Long doctorId;
    @Column(nullable=false) private Long patientId;
    @Column(nullable=false,length=160) private String filename;
    @Column(nullable=false,length=64) private String sourceSha256;
    @Column(nullable=false) private LocalDateTime createdAt;
    @Column(nullable=false,length=20) private String status="DRAFT";
    @Lob private String draftJson;
    @Lob private String confirmedRequestJson;
    private Long measurementId;
    private LocalDateTime confirmedAt;
}
