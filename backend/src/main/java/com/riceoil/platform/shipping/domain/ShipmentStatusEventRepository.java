package com.riceoil.platform.shipping.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShipmentStatusEventRepository extends JpaRepository<ShipmentStatusEventEntity, Long> {
    List<ShipmentStatusEventEntity> findByShipmentIdOrderByOccurredAtDesc(Long shipmentId);
}
