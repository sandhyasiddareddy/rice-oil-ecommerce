package com.riceoil.platform.orders.api.checkout;

import com.riceoil.platform.orders.application.OrderCheckoutService;
import com.riceoil.platform.orders.application.OrderSnapshot;
import com.riceoil.platform.orders.application.GuestOrderAccessService;
import com.riceoil.platform.orders.application.GuestOrderAccessResult;
import com.riceoil.platform.orders.domain.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CheckoutOrderController.class)
@AutoConfigureMockMvc(addFilters = false)
class CheckoutOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderCheckoutService orderCheckoutService;

    @MockitoBean
    private GuestOrderAccessService guestOrderAccessService;

    @Test
    void placeOrder_shouldReturnOrderSummary() throws Exception {
        when(guestOrderAccessService.issueToken(15L)).thenReturn(new GuestOrderAccessResult(
                15L,
                "guest-tracking-token",
                OffsetDateTime.now().plusDays(30)
        ));
        when(orderCheckoutService.placeOrder(eq("idem-1"), any(), nullable(Long.class), nullable(String.class)))
                .thenReturn(new OrderSnapshot(
                15L,
                "ORD-20261102093000-ABC123",
                9L,
                OrderStatus.CREATED,
                "INR",
                new BigDecimal("200.00"),
                new BigDecimal("10.00"),
                new BigDecimal("20.00"),
                new BigDecimal("0.00"),
                new BigDecimal("190.00"),
                "buyer@example.com"
        ));

        mockMvc.perform(post("/api/checkout/orders")
                        .header("Idempotency-Key", "idem-1")
                        .contentType("application/json")
                        .content(validPayload()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(15))
                .andExpect(jsonPath("$.cartId").value(9))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.totalAmount").value(190.00))
                .andExpect(jsonPath("$.guestAccessToken").value("guest-tracking-token"));
    }

    @Test
    void placeOrder_shouldRejectMissingCartId() throws Exception {
        mockMvc.perform(post("/api/checkout/orders")
                        .header("Idempotency-Key", "idem-1")
                        .contentType("application/json")
                        .content("{\"customerEmail\":\"buyer@example.com\"}"))
                .andExpect(status().isBadRequest());
    }

    private String validPayload() {
        return "{\"cartId\":9,\"customerEmail\":\"buyer@example.com\","
                + "\"shippingAddress\":{\"fullName\":\"Buyer\",\"phone\":\"9876543210\","
                + "\"line1\":\"1 Main Street\",\"city\":\"Chennai\",\"state\":\"Tamil Nadu\","
                + "\"postalCode\":\"600001\",\"country\":\"IN\"}}";
    }
}
