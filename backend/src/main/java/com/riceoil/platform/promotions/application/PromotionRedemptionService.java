package com.riceoil.platform.promotions.application;

import com.riceoil.platform.promotions.domain.PromotionCustomerRedemptionEntity;
import com.riceoil.platform.promotions.domain.PromotionCustomerRedemptionRepository;
import com.riceoil.platform.promotions.domain.PromotionEntity;
import com.riceoil.platform.promotions.domain.PromotionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class PromotionRedemptionService {

    private final CouponValidationService couponValidationService;
    private final PromotionRepository promotionRepository;
    private final PromotionCustomerRedemptionRepository redemptionRepository;

    public PromotionRedemptionService(CouponValidationService couponValidationService,
                                      PromotionRepository promotionRepository,
                                      PromotionCustomerRedemptionRepository redemptionRepository) {
        this.couponValidationService = couponValidationService;
        this.promotionRepository = promotionRepository;
        this.redemptionRepository = redemptionRepository;
    }

    @Transactional
    public void registerAppliedRedemptions(BigDecimal orderSubtotal,
                                           List<String> couponCodes,
                                           String customerEmail,
                                           OffsetDateTime now) {
        if (couponCodes == null || couponCodes.isEmpty()) {
            return;
        }

        CouponValidationResult result = couponValidationService.validateBestEligible(
                orderSubtotal,
                couponCodes,
                now,
                customerEmail
        );
        if (result.appliedCode() == null || result.appliedCode().isBlank()) {
            return;
        }

        String normalizedEmail = normalizeEmail(customerEmail);
        String[] appliedCodes = result.appliedCode().split(",");
        for (String raw : appliedCodes) {
            String code = raw.trim();
            if (code.isBlank()) {
                continue;
            }
            PromotionEntity promotion = promotionRepository.findByCodeIgnoreCase(code).orElse(null);
            if (promotion == null) {
                continue;
            }

            int redeemedCount = promotion.getRedeemedCount() == null ? 0 : promotion.getRedeemedCount();
            promotion.setRedeemedCount(redeemedCount + 1);
            promotionRepository.save(promotion);

            if (normalizedEmail == null) {
                continue;
            }

            PromotionCustomerRedemptionEntity customerRedemption = redemptionRepository
                    .findByPromotionIdAndCustomerEmailIgnoreCase(promotion.getId(), normalizedEmail)
                    .orElseGet(() -> {
                        PromotionCustomerRedemptionEntity created = new PromotionCustomerRedemptionEntity();
                        created.setPromotion(promotion);
                        created.setCustomerEmail(normalizedEmail);
                        created.setRedemptionCount(0);
                        return created;
                    });
            customerRedemption.setRedemptionCount(customerRedemption.getRedemptionCount() + 1);
            redemptionRepository.save(customerRedemption);
        }
    }

    private String normalizeEmail(String customerEmail) {
        if (customerEmail == null || customerEmail.isBlank()) {
            return null;
        }
        return customerEmail.trim().toLowerCase(Locale.ROOT);
    }
}
