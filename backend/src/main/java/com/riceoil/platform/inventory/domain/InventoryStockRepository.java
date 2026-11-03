package com.riceoil.platform.inventory.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InventoryStockRepository extends JpaRepository<InventoryStockEntity, Long> {

    Optional<InventoryStockEntity> findByVariantId(Long variantId);
}
