package com.riceoil.platform.shipping.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShipmentRepository extends JpaRepository<ShipmentEntity, Long> {
    List<ShipmentEntity> findByOrderIdOrderByCreatedAtAsc(Long orderId);
    Optional<ShipmentEntity> findByShipmentCode(String shipmentCode);
}
