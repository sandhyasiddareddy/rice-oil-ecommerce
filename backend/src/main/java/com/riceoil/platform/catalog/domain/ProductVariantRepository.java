package com.riceoil.platform.catalog.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductVariantRepository extends JpaRepository<ProductVariantEntity, Long> {

    Optional<ProductVariantEntity> findBySku(String sku);

    List<ProductVariantEntity> findByProductIdAndActiveTrueOrderByWeightGramsAsc(Long productId);

    List<ProductVariantEntity> findByActiveTrueOrderBySkuAsc();
}
