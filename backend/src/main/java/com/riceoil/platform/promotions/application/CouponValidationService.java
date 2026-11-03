package com.riceoil.platform.promotions.application;

import com.riceoil.platform.promotions.domain.DiscountType;
import com.riceoil.platform.promotions.domain.PromotionCustomerRedemptionEntity;
import com.riceoil.platform.promotions.domain.PromotionCustomerRedemptionRepository;
import com.riceoil.platform.promotions.domain.PromotionEntity;
import com.riceoil.platform.promotions.domain.PromotionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CouponValidationService {

    private final PromotionRepository promotionRepository;
    private final PromotionCustomerRedemptionRepository redemptionRepository;

    public CouponValidationService(PromotionRepository promotionRepository,
                                   PromotionCustomerRedemptionRepository redemptionRepository) {
        this.promotionRepository = promotionRepository;
        this.redemptionRepository = redemptionRepository;
    }

    public CouponValidationResult validateBestEligible(BigDecimal orderSubtotal,
                                                       List<String> couponCodes,
                                                       OffsetDateTime now) {
        return validateBestEligible(orderSubtotal, couponCodes, now, null);
    }

    public CouponValidationResult validateBestEligible(BigDecimal orderSubtotal,
                                                       List<String> couponCodes,
                                                       OffsetDateTime now,
                                                       String customerEmail) {
        if (orderSubtotal == null || orderSubtotal.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("orderSubtotal must be zero or greater");
        }
        if (couponCodes == null || couponCodes.isEmpty()) {
            return new CouponValidationResult(null, money(BigDecimal.ZERO), money(orderSubtotal), List.of());
        }

        List<String> rejected = new ArrayList<>();
        List<EligiblePromotion> eligible = new ArrayList<>();
        String normalizedCustomerEmail = normalizeEmail(customerEmail);

        for (String codeRaw : couponCodes) {
            String code = codeRaw == null ? "" : codeRaw.trim().toUpperCase();
            if (code.isBlank()) {
                continue;
            }

            PromotionEntity promotion = promotionRepository.findByCodeIgnoreCase(code).orElse(null);
            if (!isEligible(promotion, orderSubtotal, now, normalizedCustomerEmail)) {
                rejected.add(code);
                continue;
            }

            eligible.add(new EligiblePromotion(promotion, computeDiscount(orderSubtotal, promotion)));
        }

        if (eligible.isEmpty()) {
            return new CouponValidationResult(null, money(BigDecimal.ZERO), money(orderSubtotal), rejected);
        }

        List<EligiblePromotion> selected = selectPromotions(eligible, rejected);
        BigDecimal totalDiscount = selected.stream()
                .map(EligiblePromotion::discount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .min(orderSubtotal);

        String appliedCodes = selected.stream()
                .map(e -> e.promotion().getCode())
                .collect(Collectors.joining(","));

        BigDecimal finalAmount = money(orderSubtotal.subtract(totalDiscount));
        return new CouponValidationResult(
                appliedCodes.isBlank() ? null : appliedCodes,
                money(totalDiscount),
                finalAmount,
                rejected
        );
    }

    private List<EligiblePromotion> selectPromotions(List<EligiblePromotion> eligible,
                                                     List<String> rejected) {
        eligible.sort(Comparator.comparing(EligiblePromotion::discount).reversed());

        boolean containsNonStackable = eligible.stream().anyMatch(e -> !e.promotion().isStackable());
        if (containsNonStackable) {
            EligiblePromotion winner = eligible.get(0);
            for (int i = 1; i < eligible.size(); i++) {
                rejected.add(eligible.get(i).promotion().getCode());
            }
            return List.of(winner);
        }

        Map<String, EligiblePromotion> bestPerGroup = new HashMap<>();
        List<EligiblePromotion> selected = new ArrayList<>();
        for (EligiblePromotion candidate : eligible) {
            String group = normalizeGroup(candidate.promotion().getExclusionGroup());
            if (group == null) {
                selected.add(candidate);
                continue;
            }

            EligiblePromotion existing = bestPerGroup.get(group);
            if (existing == null) {
                bestPerGroup.put(group, candidate);
            } else {
                rejected.add(candidate.promotion().getCode());
            }
        }

        selected.addAll(bestPerGroup.values());
        return selected;
    }

    private String normalizeGroup(String group) {
        if (group == null || group.isBlank()) {
            return null;
        }
        return group.trim().toUpperCase(Locale.ROOT);
    }

    private boolean isEligible(PromotionEntity promotion,
                               BigDecimal orderSubtotal,
                               OffsetDateTime now,
                               String customerEmail) {
        if (promotion == null || !promotion.isActive()) {
            return false;
        }
        if (promotion.getStartsAt() != null && now.isBefore(promotion.getStartsAt())) {
            return false;
        }
        if (promotion.getEndsAt() != null && now.isAfter(promotion.getEndsAt())) {
            return false;
        }
        if (promotion.getMinOrderValue() != null && orderSubtotal.compareTo(promotion.getMinOrderValue()) < 0) {
            return false;
        }
        if (promotion.getMaxTotalRedemptions() != null
                && safeCount(promotion.getRedeemedCount()) >= promotion.getMaxTotalRedemptions()) {
            return false;
        }
        if (promotion.getMaxRedemptionsPerCustomer() != null) {
            if (customerEmail == null) {
                return false;
            }
            int customerCount = redemptionRepository
                    .findByPromotionIdAndCustomerEmailIgnoreCase(promotion.getId(), customerEmail)
                    .map(PromotionCustomerRedemptionEntity::getRedemptionCount)
                    .orElse(0);
            if (customerCount >= promotion.getMaxRedemptionsPerCustomer()) {
                return false;
            }
        }
        return true;
    }

    private int safeCount(Integer count) {
        return count == null ? 0 : count;
    }

    private BigDecimal computeDiscount(BigDecimal subtotal, PromotionEntity promotion) {
        if (promotion.getDiscountType() == DiscountType.PERCENTAGE) {
            BigDecimal discount = subtotal.multiply(promotion.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            return discount.min(subtotal);
        }
        if (promotion.getDiscountType() == DiscountType.FIXED_AMOUNT) {
            return promotion.getDiscountValue().min(subtotal);
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizeEmail(String customerEmail) {
        if (customerEmail == null || customerEmail.isBlank()) {
            return null;
        }
        return customerEmail.trim().toLowerCase(Locale.ROOT);
    }

    private record EligiblePromotion(PromotionEntity promotion, BigDecimal discount) {
    }
}
