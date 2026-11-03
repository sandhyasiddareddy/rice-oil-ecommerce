package com.riceoil.platform.promotions.application;

import com.riceoil.platform.promotions.domain.PromotionEntity;
import com.riceoil.platform.promotions.domain.PromotionRepository;
import com.riceoil.platform.shared.web.DuplicateResourceException;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class PromotionAdminService {

    private final PromotionRepository promotionRepository;

    public PromotionAdminService(PromotionRepository promotionRepository) {
        this.promotionRepository = promotionRepository;
    }

    public PromotionEntity create(PromotionEntity promotion) {
        String normalizedCode = promotion.getCode().trim().toUpperCase();
        if (promotionRepository.findByCodeIgnoreCase(normalizedCode).isPresent()) {
            throw new DuplicateResourceException("Promotion code already exists: " + normalizedCode);
        }
        promotion.setCode(normalizedCode);
        if (promotion.getExclusionGroup() != null && !promotion.getExclusionGroup().isBlank()) {
            promotion.setExclusionGroup(promotion.getExclusionGroup().trim().toUpperCase(Locale.ROOT));
        } else {
            promotion.setExclusionGroup(null);
        }
        if (promotion.getMaxTotalRedemptions() != null && promotion.getMaxTotalRedemptions() < 1) {
            throw new IllegalArgumentException("maxTotalRedemptions must be >= 1");
        }
        if (promotion.getMaxRedemptionsPerCustomer() != null && promotion.getMaxRedemptionsPerCustomer() < 1) {
            throw new IllegalArgumentException("maxRedemptionsPerCustomer must be >= 1");
        }
        if (promotion.getRedeemedCount() == null) {
            promotion.setRedeemedCount(0);
        }
        return promotionRepository.save(promotion);
    }
}
