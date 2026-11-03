package com.riceoil.platform.shipping.api;

import com.riceoil.platform.shipping.application.ShippingQuoteService;
import com.riceoil.platform.shipping.application.ShippingQuoteSnapshot;
import com.riceoil.platform.shipping.domain.ShippingQuoteStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ShippingQuoteController.class)
@AutoConfigureMockMvc(addFilters = false)
class ShippingQuoteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ShippingQuoteService shippingQuoteService;

    @Test
    void create_shouldReturnQuote() throws Exception {
        when(shippingQuoteService.createQuote(5L, null, "guest-cart-token")).thenReturn(snapshot());

        mockMvc.perform(post("/api/shipping/quotes")
                        .contentType("application/json")
                        .content("{\"cartId\":5}")
                        .header("X-Guest-Cart-Token", "guest-cart-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quoteCode").value("SQ-TEST123"))
                .andExpect(jsonPath("$.status").value("AVAILABLE"));
    }

    @Test
    void get_shouldReturnQuote() throws Exception {
        when(shippingQuoteService.getQuote("SQ-TEST123", null, "guest-cart-token")).thenReturn(snapshot());

        mockMvc.perform(get("/api/shipping/quotes/SQ-TEST123")
                        .header("X-Guest-Cart-Token", "guest-cart-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cartId").value(5));
    }

    private ShippingQuoteSnapshot snapshot() {
        return new ShippingQuoteSnapshot(
                "SQ-TEST123",
                5L,
                ShippingQuoteStatus.AVAILABLE,
                new BigDecimal("40.00"),
                "INR",
                "SHIPROCKET",
                "STANDARD",
                OffsetDateTime.parse("2026-11-02T14:00:00+05:30")
        );
    }
}
