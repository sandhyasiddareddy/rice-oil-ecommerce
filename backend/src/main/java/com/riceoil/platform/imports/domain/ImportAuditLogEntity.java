package com.riceoil.platform.imports.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "import_audit_log")
public class ImportAuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private ImportJobEntity job;

    @Column(nullable = false, length = 60)
    private String action;

    @Column(nullable = false, length = 120)
    private String actor;

    @Column
    private String details;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }

    public void setJob(ImportJobEntity job) {
        this.job = job;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}
