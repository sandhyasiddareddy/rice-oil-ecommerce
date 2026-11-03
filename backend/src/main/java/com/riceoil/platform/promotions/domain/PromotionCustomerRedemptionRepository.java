package com.riceoil.platform.promotions.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PromotionCustomerRedemptionRepository extends JpaRepository<PromotionCustomerRedemptionEntity, Long> {

    Optional<PromotionCustomerRedemptionEntity> findByPromotionIdAndCustomerEmailIgnoreCase(Long promotionId,
                                                                                             String customerEmail);
}
