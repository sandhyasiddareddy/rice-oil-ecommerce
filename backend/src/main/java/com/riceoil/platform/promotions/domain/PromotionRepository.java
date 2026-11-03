package com.riceoil.platform.promotions.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PromotionRepository extends JpaRepository<PromotionEntity, Long> {

    Optional<PromotionEntity> findByCodeIgnoreCase(String code);

    List<PromotionEntity> findByCodeIgnoreCaseIn(List<String> codes);
}
