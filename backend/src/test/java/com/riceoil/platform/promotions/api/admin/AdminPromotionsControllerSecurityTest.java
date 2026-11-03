package com.riceoil.platform.promotions.api.admin;

import com.riceoil.platform.promotions.application.PromotionAdminService;
import com.riceoil.platform.promotions.domain.DiscountType;
import com.riceoil.platform.promotions.domain.PromotionEntity;
import com.riceoil.platform.shared.web.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminPromotionsController.class)
@Import(SecurityConfig.class)
class AdminPromotionsControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PromotionAdminService promotionAdminService;

    @Test
    void unauthenticatedCreate_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/api/admin/promotions")
                        .contentType("application/json")
                        .content(validPayload()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "operator", roles = "USER")
    void nonAdminCreate_shouldReturnForbidden() throws Exception {
        mockMvc.perform(post("/api/admin/promotions")
                        .contentType("application/json")
                        .content(validPayload()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCreate_shouldSucceed() throws Exception {
        PromotionEntity entity = new PromotionEntity();
        entity.setCode("SAVE20");
        entity.setDiscountType(DiscountType.FIXED_AMOUNT);
        entity.setDiscountValue(new BigDecimal("20.00"));
        entity.setStackable(false);
        entity.setRedeemedCount(0);
        entity.setActive(true);

        when(promotionAdminService.create(any(PromotionEntity.class))).thenReturn(entity);

        mockMvc.perform(post("/api/admin/promotions")
                        .contentType("application/json")
                        .content(validPayload()))
                .andExpect(status().isOk());
    }

    private String validPayload() {
        return "{\"code\":\"save20\",\"discountType\":\"FIXED_AMOUNT\",\"discountValue\":20,\"active\":true}";
    }
}
