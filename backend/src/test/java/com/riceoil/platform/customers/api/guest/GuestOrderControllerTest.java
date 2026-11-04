package com.riceoil.platform.customers.api.guest;

import com.riceoil.platform.orders.application.GuestOrderAccessService;
import com.riceoil.platform.orders.application.GuestOrderAccessSnapshot;
import com.riceoil.platform.orders.application.OrderItemsQueryService;
import com.riceoil.platform.orders.application.OrderLinesSnapshot;
import com.riceoil.platform.orders.application.aftercare.AftercareService;
import com.riceoil.platform.orders.application.aftercare.AftercareSummary;
import com.riceoil.platform.orders.domain.OrderStatus;
import com.riceoil.platform.shipping.application.ShipmentTrackingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GuestOrderController.class)
@AutoConfigureMockMvc(addFilters = false)
class GuestOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GuestOrderAccessService guestOrderAccessService;

    @MockitoBean
    private ShipmentTrackingService shipmentTrackingService;

    @MockitoBean
    private AftercareService aftercareService;

    @MockitoBean
    private OrderItemsQueryService orderItemsQueryService;

    @org.junit.jupiter.api.BeforeEach
    void stubLines() {
        when(orderItemsQueryService.linesFor(7L)).thenReturn(new OrderLinesSnapshot(
                java.util.List.of(new com.riceoil.platform.orders.application.OrderItemSnapshot(
                        "RICE-1KG", "Rice Bran Oil", 2, new java.math.BigDecimal("75.00"),
                        new java.math.BigDecimal("150.00"))),
                new java.math.BigDecimal("40.00")));
    }

    @Test
    void guestCannotIssueAccessTokenForArbitraryOrder() throws Exception {
        mockMvc.perform(post("/api/guest/orders/access")
                        .contentType("application/json")
                        .content("{\"orderId\":7}"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void orderStatus_shouldReturnStatus() throws Exception {
        when(guestOrderAccessService.verifyAndGetOrder(7L, "guest-token"))
                .thenReturn(new GuestOrderAccessSnapshot(7L, "ORD-7", OrderStatus.PAID, "INR",
                        new java.math.BigDecimal("190.00")));
        when(aftercareService.summary(7L)).thenReturn(new AftercareSummary(true, null, null, null));

        mockMvc.perform(get("/api/guest/orders/7")                        .header("X-Guest-Token", "guest-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.orderNumber").value("ORD-7"));
    }

    @Test
    void tracking_shouldIncludeShipmentHistory() throws Exception {
        when(guestOrderAccessService.verifyAndGetOrder(7L, "guest-token"))
                .thenReturn(new GuestOrderAccessSnapshot(7L, "ORD-7", OrderStatus.PAID, "INR",
                        new java.math.BigDecimal("190.00")));
        when(shipmentTrackingService.getOrderShipments(7L)).thenReturn(java.util.List.of());
        when(aftercareService.summary(7L)).thenReturn(new AftercareSummary(true, "REJECTED", "PROCESSED",
                new java.math.BigDecimal("190.00")));

        mockMvc.perform(get("/api/guest/orders/7/tracking")
                        .header("X-Guest-Token", "guest-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shipments").isArray())
                .andExpect(jsonPath("$.items[0].productName").value("Rice Bran Oil"))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.shippingAmount").value(40.00))
                .andExpect(jsonPath("$.canRequestCancellation").value(true))
                .andExpect(jsonPath("$.cancellationStatus").value("REJECTED"))
                .andExpect(jsonPath("$.refundStatus").value("PROCESSED"))
                .andExpect(jsonPath("$.refundAmount").value(190.00));
    }
}
