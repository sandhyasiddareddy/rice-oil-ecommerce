package com.riceoil.platform.payments.api;

import com.riceoil.platform.orders.domain.OrderStatus;
import com.riceoil.platform.payments.application.PaymentOrderService;
import com.riceoil.platform.payments.application.PaymentVerificationService;
import com.riceoil.platform.payments.application.RazorpayCreateOrderResult;
import com.riceoil.platform.payments.application.RazorpayVerifyResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RazorpayPaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
class RazorpayPaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentOrderService paymentOrderService;

    @MockitoBean
    private PaymentVerificationService paymentVerificationService;

    @Test
    void createOrder_shouldReturnLinkedPaymentOrder() throws Exception {
        when(paymentOrderService.createRazorpayOrder(9L, null, "guest-token")).thenReturn(new RazorpayCreateOrderResult(
                9L,
                "rpay_abc123",
                "INR",
                19000L,
                OrderStatus.PAYMENT_PENDING,
                "rzp_test_public"
        ));

        mockMvc.perform(post("/api/payments/razorpay/create-order")
                        .contentType("application/json")
                        .content("{\"orderId\":9}")
                        .header("X-Guest-Order-Token", "guest-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(9))
                .andExpect(jsonPath("$.providerOrderId").value("rpay_abc123"))
                .andExpect(jsonPath("$.amount").value(19000))
                .andExpect(jsonPath("$.keyId").value("rzp_test_public"));
    }

    @Test
    void verify_shouldReturnPaidStatus() throws Exception {
        when(paymentVerificationService.verify(9L, "pay_123", "sig_abc", null, "guest-token")).thenReturn(
                new RazorpayVerifyResult(9L, "pay_123", OrderStatus.PAID, true)
        );

        mockMvc.perform(post("/api/payments/razorpay/verify")
                        .contentType("application/json")
                        .content("{\"orderId\":9,\"paymentId\":\"pay_123\",\"signature\":\"sig_abc\"}")
                        .header("X-Guest-Order-Token", "guest-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.verified").value(true));
    }

    @Test
    void webhook_shouldAcceptAndReturnOk() throws Exception {
        doNothing().when(paymentVerificationService).processWebhook(anyString(), anyString());

        mockMvc.perform(post("/api/payments/razorpay/webhook")
                        .contentType("application/json")
                        .header("X-Razorpay-Signature", "sig_abc")
                        .content("{\"id\":\"evt_1\",\"event\":\"payment.captured\",\"payload\":{\"payment\":{\"entity\":{\"id\":\"pay_123\",\"order_id\":\"rpay_abc123\"}}}}"))
                .andExpect(status().isOk());
    }
}
