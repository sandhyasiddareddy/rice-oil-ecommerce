package com.riceoil.platform.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "inventory_adjustment")
public class InventoryAdjustmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "variant_id", nullable = false)
    private Long variantId;

    @Column(name = "delta_qty", nullable = false)
    private int deltaQty;

    @Column(nullable = false, length = 120)
    private String reason;

    @Column(length = 120)
    private String reference;

    @Column(name = "created_by", nullable = false, length = 120)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }

    public void setVariantId(Long variantId) {
        this.variantId = variantId;
    }

    public void setDeltaQty(int deltaQty) {
        this.deltaQty = deltaQty;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}
