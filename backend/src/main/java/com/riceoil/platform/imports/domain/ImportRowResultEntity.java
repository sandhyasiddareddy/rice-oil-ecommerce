package com.riceoil.platform.imports.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "import_row_result")
public class ImportRowResultEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private ImportJobEntity job;

    @Column(name = "row_number", nullable = false)
    private int rowNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ImportRowStatus status;

    @Column(name = "error_code", length = 80)
    private String errorCode;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "payload_json")
    private String payloadJson;

    public void setJob(ImportJobEntity job) {
        this.job = job;
    }

    public void setRowNumber(int rowNumber) {
        this.rowNumber = rowNumber;
    }

    public int getRowNumber() {
        return rowNumber;
    }

    public void setStatus(ImportRowStatus status) {
        this.status = status;
    }

    public ImportRowStatus getStatus() {
        return status;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setPayloadJson(String payloadJson) {
        this.payloadJson = payloadJson;
    }

    public String getPayloadJson() {
        return payloadJson;
    }
}
