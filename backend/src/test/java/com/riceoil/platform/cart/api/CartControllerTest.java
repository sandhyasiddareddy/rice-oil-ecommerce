package com.riceoil.platform.cart.api;

import com.riceoil.platform.cart.application.CartApplicationService;
import com.riceoil.platform.cart.application.CartItemSnapshot;
import com.riceoil.platform.cart.application.CartSnapshot;
import com.riceoil.platform.cart.domain.CartStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CartController.class)
@AutoConfigureMockMvc(addFilters = false)
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CartApplicationService cartApplicationService;

    @Test
    void createCart_shouldReturnCart() throws Exception {
        when(cartApplicationService.createCart(null)).thenReturn(snapshot(5L));

        mockMvc.perform(post("/api/carts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.currency").value("INR"));
    }

    @Test
    void getCart_shouldReturnCart() throws Exception {
        when(cartApplicationService.getCart(5L, null, null)).thenReturn(snapshot(5L));

        mockMvc.perform(get("/api/carts/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.items[0].sku").value("RICE-5KG"));
    }

    @Test
    void addItem_shouldReturnUpdatedCart() throws Exception {
        when(cartApplicationService.addItem(eq(5L), any(), isNull(), isNull())).thenReturn(snapshot(5L));

        mockMvc.perform(post("/api/carts/5/items")
                        .contentType("application/json")
                        .content("{\"sku\":\"RICE-5KG\",\"productName\":\"Rice\",\"unitPrice\":100,\"quantity\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].lineTotal").value(200.00));
    }

    @Test
    void refreshPricing_shouldReturnRepricedCart() throws Exception {
        when(cartApplicationService.refreshPricing(eq(5L), any(), isNull(), isNull())).thenReturn(snapshot(5L));

        mockMvc.perform(post("/api/carts/5/pricing/refresh")
                        .contentType("application/json")
                        .content("{\"taxCode\":\"GST5\",\"couponCodes\":[\"SAVE20\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taxAmount").value(10.00))
                .andExpect(jsonPath("$.totalAmount").value(190.00));
    }

    @Test
    void updateItem_shouldReturnUpdatedCart() throws Exception {
        when(cartApplicationService.updateItem(eq(5L), eq(11L), any(), isNull(), isNull())).thenReturn(snapshot(5L));

        mockMvc.perform(patch("/api/carts/5/items/11")
                        .contentType("application/json")
                        .content("{\"unitPrice\":120,\"quantity\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(11));
    }

    @Test
    void deleteItem_shouldReturnUpdatedCart() throws Exception {
        when(cartApplicationService.removeItem(5L, 11L, null, null)).thenReturn(snapshot(5L));

        mockMvc.perform(delete("/api/carts/5/items/11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5));
    }

    private CartSnapshot snapshot(Long id) {
        return new CartSnapshot(
                id,
                CartStatus.ACTIVE,
                "INR",
                "GST5",
                new BigDecimal("200.00"),
                new BigDecimal("10.00"),
                new BigDecimal("20.00"),
                new BigDecimal("190.00"),
                List.of("SAVE20"),
                List.of(new CartItemSnapshot(
                        11L,
                        "RICE-5KG",
                        "Rice",
                        new BigDecimal("100.00"),
                        2,
                        new BigDecimal("200.00")
                ))
        );
    }
}
