package com.riceoil.platform.payments.application;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RazorpayHttpGatewayRefundTest {

    @Test
    void createRefund_shouldPostCapturedAmountToRazorpay() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RazorpayHttpGateway gateway = new RazorpayHttpGateway(
                builder, "https://api.razorpay.test/v1", "key-id", "key-secret");

        server.expect(requestTo("https://api.razorpay.test/v1/payments/pay_123/refund"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Basic a2V5LWlkOmtleS1zZWNyZXQ="))
                .andExpect(jsonPath("$.amount").value(12900))
                .andExpect(jsonPath("$.notes.farmora_refund_request_id").value("refund-5"))
                .andRespond(withSuccess("{\"id\":\"rfnd_123\",\"status\":\"pending\"}", MediaType.APPLICATION_JSON));

        RazorpayRefundDetails result = gateway.createRefund("pay_123", 12900, "refund-5");

        assertEquals("rfnd_123", result.id());
        assertEquals("pending", result.status());
        server.verify();
    }
}
