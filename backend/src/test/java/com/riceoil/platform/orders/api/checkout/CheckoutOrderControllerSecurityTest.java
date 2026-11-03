package com.riceoil.platform.orders.api.checkout;

import com.riceoil.platform.orders.application.OrderCheckoutService;
import com.riceoil.platform.orders.application.OrderSnapshot;
import com.riceoil.platform.orders.application.GuestOrderAccessService;
import com.riceoil.platform.orders.application.GuestOrderAccessResult;
import com.riceoil.platform.orders.domain.OrderStatus;
import com.riceoil.platform.shared.web.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CheckoutOrderController.class)
@Import(SecurityConfig.class)
class CheckoutOrderControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderCheckoutService orderCheckoutService;

    @MockitoBean
    private GuestOrderAccessService guestOrderAccessService;

    @Test
        void unauthenticatedPlaceOrder_shouldSucceedForGuestCheckout() throws Exception {
            when(guestOrderAccessService.issueToken(15L)).thenReturn(new GuestOrderAccessResult(
                    15L,
                    "guest-tracking-token",
                    OffsetDateTime.now().plusDays(30)
            ));
        when(orderCheckoutService.placeOrder(anyString(), any(), nullable(Long.class), nullable(String.class)))
                .thenReturn(new OrderSnapshot(
            15L,
            "ORD-20261102093000-ABC123",
            9L,
            OrderStatus.CREATED,
            "INR",
            new BigDecimal("200.00"),
            new BigDecimal("10.00"),
            new BigDecimal("20.00"),
            new BigDecimal("190.00"),
            "buyer@example.com"
        ));

        mockMvc.perform(post("/api/checkout/orders")
                        .header("Idempotency-Key", "idem-1")
                        .contentType("application/json")
                        .content(validPayload()))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "buyer", roles = "USER")
    void authenticatedPlaceOrder_shouldSucceed() throws Exception {
        when(guestOrderAccessService.issueToken(15L)).thenReturn(new GuestOrderAccessResult(
                15L,
                "guest-tracking-token",
                OffsetDateTime.now().plusDays(30)
        ));
        when(orderCheckoutService.placeOrder(anyString(), any(), nullable(Long.class), nullable(String.class)))
                .thenReturn(new OrderSnapshot(
                15L,
                "ORD-20261102093000-ABC123",
                9L,
                OrderStatus.CREATED,
                "INR",
                new BigDecimal("200.00"),
                new BigDecimal("10.00"),
                new BigDecimal("20.00"),
                new BigDecimal("190.00"),
                "buyer@example.com"
        ));

        mockMvc.perform(post("/api/checkout/orders")
                        .header("Idempotency-Key", "idem-1")
                        .contentType("application/json")
                        .content(validPayload()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "buyer", roles = "USER")
    void missingIdempotencyKey_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/checkout/orders")
                        .contentType("application/json")
                        .content(validPayload()))
                .andExpect(status().isBadRequest());
    }

    private String validPayload() {
        return "{\"cartId\":9,\"customerEmail\":\"buyer@example.com\"}";
    }
}
