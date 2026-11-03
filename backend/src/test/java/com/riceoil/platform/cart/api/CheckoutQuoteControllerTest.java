package com.riceoil.platform.cart.api;

import com.riceoil.platform.cart.application.CheckoutQuoteRequest;
import com.riceoil.platform.cart.application.CheckoutQuoteResult;
import com.riceoil.platform.cart.application.CheckoutQuoteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CheckoutQuoteController.class)
@AutoConfigureMockMvc(addFilters = false)
class CheckoutQuoteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CheckoutQuoteService checkoutQuoteService;

    @Test
    void quote_shouldReturnPricingSummary() throws Exception {
        when(checkoutQuoteService.quote(any(CheckoutQuoteRequest.class)))
                .thenReturn(new CheckoutQuoteResult(
                        new BigDecimal("100.00"),
                        new BigDecimal("5.00"),
                        new BigDecimal("105.00"),
                        "GST5",
                        false,
                        "SAVE20",
                        new BigDecimal("20.00"),
                        new BigDecimal("85.00"),
                        List.of()
                ));

        mockMvc.perform(post("/api/checkout/quote")
                        .contentType("application/json")
                        .content("{\"subtotal\":100,\"taxCode\":\"GST5\",\"couponCodes\":[\"SAVE20\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.baseAmount").value(100.00))
                .andExpect(jsonPath("$.taxAmount").value(5.00))
                .andExpect(jsonPath("$.preDiscountTotal").value(105.00))
                .andExpect(jsonPath("$.appliedCoupon").value("SAVE20"))
                .andExpect(jsonPath("$.discountAmount").value(20.00))
                .andExpect(jsonPath("$.finalAmount").value(85.00));
    }

    @Test
    void quote_shouldRejectNegativeSubtotal() throws Exception {
        mockMvc.perform(post("/api/checkout/quote")
                        .contentType("application/json")
                        .content("{\"subtotal\":-1,\"taxCode\":\"GST5\"}"))
                .andExpect(status().isBadRequest());
    }
}
