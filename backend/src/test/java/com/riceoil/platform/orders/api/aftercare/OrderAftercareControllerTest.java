package com.riceoil.platform.orders.api.aftercare;

import com.riceoil.platform.orders.application.aftercare.AftercareService;
import com.riceoil.platform.orders.application.aftercare.CancellationSnapshot;
import com.riceoil.platform.orders.application.aftercare.RefundSnapshot;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderAftercareController.class)
@AutoConfigureMockMvc(addFilters = false)
class OrderAftercareControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AftercareService aftercareService;

    @Test
    void cancellation_shouldCreateRequest() throws Exception {
        when(aftercareService.requestCancellation(eq(8L), eq("Wrong item")))
                .thenReturn(new CancellationSnapshot(1L, 8L, "REQUESTED", "Wrong item"));

        mockMvc.perform(post("/api/orders/8/cancellation-requests")
                        .contentType("application/json")
                        .content("{\"reason\":\"Wrong item\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REQUESTED"));
    }

    @Test
    void refund_shouldCreateRequest() throws Exception {
        when(aftercareService.requestRefund(
                eq(8L),
                argThat(amount -> amount != null && amount.compareTo(new BigDecimal("90.00")) == 0),
                eq("Damaged")
        ))
                .thenReturn(new RefundSnapshot(2L, 8L, new BigDecimal("90.00"), "REQUESTED", "Damaged"));

        mockMvc.perform(post("/api/orders/8/refund-requests")
                        .contentType("application/json")
                        .content("{\"amount\":90,\"reason\":\"Damaged\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(90.00));
    }

    @Test
    void refunds_shouldReturnHistory() throws Exception {
        when(aftercareService.refunds(8L)).thenReturn(List.of(
                new RefundSnapshot(2L, 8L, new BigDecimal("90.00"), "REQUESTED", "Damaged")
        ));

        mockMvc.perform(get("/api/orders/8/refunds"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("REQUESTED"));
    }
}
