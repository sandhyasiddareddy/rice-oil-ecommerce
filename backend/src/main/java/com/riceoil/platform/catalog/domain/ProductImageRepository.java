package com.riceoil.platform.catalog.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductImageRepository extends JpaRepository<ProductImageEntity, Long> {

    Optional<ProductImageEntity> findFirstByProductIdAndPublishedTrueOrderBySortOrderAsc(Long productId);

    Optional<ProductImageEntity> findFirstByProductIdOrderBySortOrderDescIdDesc(Long productId);
}
