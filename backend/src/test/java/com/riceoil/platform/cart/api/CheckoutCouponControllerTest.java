package com.riceoil.platform.cart.api;

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

@WebMvcTest(CheckoutCouponController.class)
@AutoConfigureMockMvc(addFilters = false)
class CheckoutCouponControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CheckoutQuoteService checkoutQuoteService;

    @Test
    void apply_shouldReturnCouponOutcome() throws Exception {
        when(checkoutQuoteService.quote(any())).thenReturn(new CheckoutQuoteResult(
                new BigDecimal("200.00"),
                new BigDecimal("10.00"),
                new BigDecimal("210.00"),
                "GST5",
                false,
                "SAVE20",
                new BigDecimal("20.00"),
                new BigDecimal("190.00"),
                List.of("OLD10")
        ));

        mockMvc.perform(post("/api/checkout/coupons/apply")
                        .contentType("application/json")
                        .content("{\"subtotal\":200,\"taxCode\":\"GST5\",\"couponCodes\":[\"SAVE20\",\"OLD10\"],\"customerEmail\":\"buyer@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appliedCoupons").value("SAVE20"))
                .andExpect(jsonPath("$.discountAmount").value(20.00))
                .andExpect(jsonPath("$.finalAmount").value(190.00));
    }
}
