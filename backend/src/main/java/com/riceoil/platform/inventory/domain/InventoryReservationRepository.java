package com.riceoil.platform.inventory.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservationEntity, Long> {
    List<InventoryReservationEntity> findByStatusAndExpiresAtBefore(String status, OffsetDateTime expiresAt);
}
