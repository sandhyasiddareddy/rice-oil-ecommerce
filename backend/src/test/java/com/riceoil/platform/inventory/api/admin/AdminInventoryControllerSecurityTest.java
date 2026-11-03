package com.riceoil.platform.inventory.api.admin;

import com.riceoil.platform.inventory.application.InventoryCommandService;
import com.riceoil.platform.inventory.domain.InventoryStockEntity;
import com.riceoil.platform.shared.web.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminInventoryController.class)
@Import(SecurityConfig.class)
class AdminInventoryControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InventoryCommandService inventoryCommandService;

    @Test
    void unauthenticatedAdjustment_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/api/admin/inventory/adjustments")
                        .contentType("application/json")
                        .content("{\"variantId\":11,\"deltaQty\":5,\"reason\":\"AUDIT\",\"reference\":\"ref-1\",\"createdBy\":\"ops@example.com\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "operator", roles = "USER")
    void nonAdminAdjustment_shouldReturnForbidden() throws Exception {
        mockMvc.perform(post("/api/admin/inventory/adjustments")
                        .contentType("application/json")
                        .content("{\"variantId\":11,\"deltaQty\":5,\"reason\":\"AUDIT\",\"reference\":\"ref-1\",\"createdBy\":\"ops@example.com\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminAdjustment_shouldSucceed() throws Exception {
        InventoryStockEntity stock = new InventoryStockEntity();
        stock.setVariantId(11L);
        stock.setAvailableQty(45);
        stock.setReservedQty(3);

        when(inventoryCommandService.adjustStock(11L, 5, "AUDIT", "ref-1", "ops@example.com"))
                .thenReturn(stock);

        mockMvc.perform(post("/api/admin/inventory/adjustments")
                        .contentType("application/json")
                        .content("{\"variantId\":11,\"deltaQty\":5,\"reason\":\"AUDIT\",\"reference\":\"ref-1\",\"createdBy\":\"ops@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.variantId").value(11))
                .andExpect(jsonPath("$.availableQty").value(45))
                .andExpect(jsonPath("$.reservedQty").value(3));
    }
}
