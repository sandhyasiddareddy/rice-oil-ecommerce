package com.riceoil.platform.promotions.application;

import com.riceoil.platform.promotions.domain.DiscountType;
import com.riceoil.platform.promotions.domain.PromotionCustomerRedemptionEntity;
import com.riceoil.platform.promotions.domain.PromotionCustomerRedemptionRepository;
import com.riceoil.platform.promotions.domain.PromotionEntity;
import com.riceoil.platform.promotions.domain.PromotionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouponValidationServiceTest {

    @Mock
    private PromotionRepository promotionRepository;

    @Mock
    private PromotionCustomerRedemptionRepository redemptionRepository;

    @InjectMocks
    private CouponValidationService couponValidationService;

    @Test
    void validateBestEligible_shouldPickBestDiscountOnly() {
        PromotionEntity tenPercent = new PromotionEntity();
        tenPercent.setCode("SAVE10");
        tenPercent.setDiscountType(DiscountType.PERCENTAGE);
        tenPercent.setDiscountValue(new BigDecimal("10"));
        tenPercent.setActive(true);

        PromotionEntity fixedTwenty = new PromotionEntity();
        fixedTwenty.setCode("SAVE20");
        fixedTwenty.setDiscountType(DiscountType.FIXED_AMOUNT);
        fixedTwenty.setDiscountValue(new BigDecimal("20"));
        fixedTwenty.setActive(true);

        when(promotionRepository.findByCodeIgnoreCase("SAVE10")).thenReturn(Optional.of(tenPercent));
        when(promotionRepository.findByCodeIgnoreCase("SAVE20")).thenReturn(Optional.of(fixedTwenty));

        CouponValidationResult result = couponValidationService.validateBestEligible(
                new BigDecimal("150.00"),
                List.of("SAVE10", "SAVE20"),
                OffsetDateTime.now()
        );

        assertEquals("SAVE20", result.appliedCode());
        assertEquals(new BigDecimal("20.00"), result.discountAmount());
        assertEquals(new BigDecimal("130.00"), result.finalAmount());
    }

    @Test
    void validateBestEligible_shouldRejectMissingOrInactiveCodes() {
        PromotionEntity inactive = new PromotionEntity();
        inactive.setCode("OLD5");
        inactive.setDiscountType(DiscountType.FIXED_AMOUNT);
        inactive.setDiscountValue(new BigDecimal("5"));
        inactive.setActive(false);

        when(promotionRepository.findByCodeIgnoreCase("MISSING")).thenReturn(Optional.empty());
        when(promotionRepository.findByCodeIgnoreCase("OLD5")).thenReturn(Optional.of(inactive));

        CouponValidationResult result = couponValidationService.validateBestEligible(
                new BigDecimal("100.00"),
                List.of("MISSING", "OLD5"),
                OffsetDateTime.now()
        );

        assertEquals(null, result.appliedCode());
        assertEquals(new BigDecimal("0.00"), result.discountAmount());
        assertEquals(new BigDecimal("100.00"), result.finalAmount());
        assertEquals(List.of("MISSING", "OLD5"), result.rejectedCodes());
    }

    @Test
    void validateBestEligible_shouldApplyStackableAcrossGroups() throws Exception {
        PromotionEntity food10 = new PromotionEntity();
        setPrivateId(food10, 1L);
        food10.setCode("FOOD10");
        food10.setDiscountType(DiscountType.PERCENTAGE);
        food10.setDiscountValue(new BigDecimal("10"));
        food10.setStackable(true);
        food10.setExclusionGroup("FOOD");
        food10.setActive(true);
        food10.setRedeemedCount(0);

        PromotionEntity ship5 = new PromotionEntity();
        setPrivateId(ship5, 2L);
        ship5.setCode("SHIP5");
        ship5.setDiscountType(DiscountType.FIXED_AMOUNT);
        ship5.setDiscountValue(new BigDecimal("5"));
        ship5.setStackable(true);
        ship5.setExclusionGroup("SHIP");
        ship5.setActive(true);
        ship5.setRedeemedCount(0);

        when(promotionRepository.findByCodeIgnoreCase("FOOD10")).thenReturn(Optional.of(food10));
        when(promotionRepository.findByCodeIgnoreCase("SHIP5")).thenReturn(Optional.of(ship5));

        CouponValidationResult result = couponValidationService.validateBestEligible(
                new BigDecimal("100.00"),
                List.of("FOOD10", "SHIP5"),
                OffsetDateTime.now(),
                "buyer@example.com"
        );

        Set<String> applied = Arrays.stream(result.appliedCode().split(","))
            .map(String::trim)
            .collect(Collectors.toSet());
        assertEquals(Set.of("FOOD10", "SHIP5"), applied);
        assertEquals(new BigDecimal("15.00"), result.discountAmount());
    }

    @Test
    void validateBestEligible_shouldRejectWhenCustomerLimitReached() throws Exception {
        PromotionEntity limited = new PromotionEntity();
        setPrivateId(limited, 3L);
        limited.setCode("LIMIT1");
        limited.setDiscountType(DiscountType.FIXED_AMOUNT);
        limited.setDiscountValue(new BigDecimal("10"));
        limited.setStackable(false);
        limited.setMaxRedemptionsPerCustomer(1);
        limited.setActive(true);
        limited.setRedeemedCount(0);

        PromotionCustomerRedemptionEntity redemption = new PromotionCustomerRedemptionEntity();
        redemption.setRedemptionCount(1);

        when(promotionRepository.findByCodeIgnoreCase("LIMIT1")).thenReturn(Optional.of(limited));
        when(redemptionRepository.findByPromotionIdAndCustomerEmailIgnoreCase(3L, "buyer@example.com"))
                .thenReturn(Optional.of(redemption));

        CouponValidationResult result = couponValidationService.validateBestEligible(
                new BigDecimal("100.00"),
                List.of("LIMIT1"),
                OffsetDateTime.now(),
                "buyer@example.com"
        );

        assertEquals(null, result.appliedCode());
        assertEquals(List.of("LIMIT1"), result.rejectedCodes());
    }

    private void setPrivateId(Object target, Long id) throws Exception {
        var idField = target.getClass().getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(target, id);
    }
}
