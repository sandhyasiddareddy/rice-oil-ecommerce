package com.riceoil.platform.promotions.api.admin;

import com.riceoil.platform.promotions.application.PromotionAdminService;
import com.riceoil.platform.promotions.domain.DiscountType;
import com.riceoil.platform.promotions.domain.PromotionEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminPromotionsController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminPromotionsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PromotionAdminService promotionAdminService;

    @Test
    void create_shouldReturnCreatedPromotion() throws Exception {
        PromotionEntity entity = new PromotionEntity();
        setPrivateId(entity, 91L);
        entity.setCode("SAVE20");
        entity.setDiscountType(DiscountType.FIXED_AMOUNT);
        entity.setDiscountValue(new BigDecimal("20.00"));
        entity.setMinOrderValue(new BigDecimal("100.00"));
        entity.setStackable(true);
        entity.setExclusionGroup("FOOD");
        entity.setMaxTotalRedemptions(100);
        entity.setMaxRedemptionsPerCustomer(2);
        entity.setRedeemedCount(0);
        entity.setActive(true);
        entity.setStartsAt(OffsetDateTime.parse("2026-11-02T09:00:00+05:30"));
        entity.setEndsAt(OffsetDateTime.parse("2026-12-02T09:00:00+05:30"));

        when(promotionAdminService.create(any(PromotionEntity.class))).thenReturn(entity);

        mockMvc.perform(post("/api/admin/promotions")
                        .contentType("application/json")
                .content("{\"code\":\"save20\",\"discountType\":\"FIXED_AMOUNT\",\"discountValue\":20,\"minOrderValue\":100,\"stackable\":true,\"exclusionGroup\":\"FOOD\",\"maxTotalRedemptions\":100,\"maxRedemptionsPerCustomer\":2,\"active\":true,\"startsAt\":\"2026-11-02T09:00:00+05:30\",\"endsAt\":\"2026-12-02T09:00:00+05:30\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(91))
                .andExpect(jsonPath("$.code").value("SAVE20"))
                .andExpect(jsonPath("$.discountType").value("FIXED_AMOUNT"))
                .andExpect(jsonPath("$.discountValue").value(20.00))
                .andExpect(jsonPath("$.minOrderValue").value(100.00))
            .andExpect(jsonPath("$.stackable").value(true))
            .andExpect(jsonPath("$.exclusionGroup").value("FOOD"))
                .andExpect(jsonPath("$.active").value(true));

        verify(promotionAdminService).create(any(PromotionEntity.class));
    }

    @Test
    void create_shouldRejectInvalidPayload() throws Exception {
        mockMvc.perform(post("/api/admin/promotions")
                        .contentType("application/json")
                        .content("{\"code\":\"\",\"discountType\":null,\"discountValue\":null,\"active\":true}"))
                .andExpect(status().isBadRequest());
    }

    private void setPrivateId(Object target, Long id) throws Exception {
        var idField = target.getClass().getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(target, id);
    }
}
