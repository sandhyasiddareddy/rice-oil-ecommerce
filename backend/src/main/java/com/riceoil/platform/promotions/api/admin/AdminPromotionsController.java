package com.riceoil.platform.promotions.api.admin;

import com.riceoil.platform.promotions.application.PromotionAdminService;
import com.riceoil.platform.promotions.domain.PromotionEntity;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/promotions")
public class AdminPromotionsController {

    private final PromotionAdminService promotionAdminService;

    public AdminPromotionsController(PromotionAdminService promotionAdminService) {
        this.promotionAdminService = promotionAdminService;
    }

    @PostMapping
    public PromotionResponse create(@Valid @RequestBody CreatePromotionRequest request) {
        PromotionEntity promotion = new PromotionEntity();
        promotion.setCode(request.code());
        promotion.setDiscountType(request.discountType());
        promotion.setDiscountValue(request.discountValue());
        promotion.setMinOrderValue(request.minOrderValue());
        promotion.setStackable(request.stackable());
        promotion.setExclusionGroup(request.exclusionGroup());
        promotion.setMaxTotalRedemptions(request.maxTotalRedemptions());
        promotion.setMaxRedemptionsPerCustomer(request.maxRedemptionsPerCustomer());
        promotion.setActive(request.active());
        promotion.setStartsAt(request.startsAt());
        promotion.setEndsAt(request.endsAt());

        PromotionEntity saved = promotionAdminService.create(promotion);
        return new PromotionResponse(
                saved.getId(),
                saved.getCode(),
                saved.getDiscountType(),
                saved.getDiscountValue(),
                saved.getMinOrderValue(),
                saved.isStackable(),
                saved.getExclusionGroup(),
                saved.getMaxTotalRedemptions(),
                saved.getMaxRedemptionsPerCustomer(),
                saved.getRedeemedCount(),
                saved.isActive(),
                saved.getStartsAt(),
                saved.getEndsAt()
        );
    }
}
