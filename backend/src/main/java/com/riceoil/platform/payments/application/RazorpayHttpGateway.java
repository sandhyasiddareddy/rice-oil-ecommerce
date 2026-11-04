package com.riceoil.platform.payments.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class RazorpayHttpGateway implements RazorpayGateway {

    private final RestClient restClient;
    private final String keyId;
    private final String keySecret;

    public RazorpayHttpGateway(RestClient.Builder restClientBuilder,
                               @Value("${app.payments.razorpay.base-url:https://api.razorpay.com/v1}") String baseUrl,
                               @Value("${app.payments.razorpay.key-id:}") String keyId,
                               @Value("${app.payments.razorpay.key-secret:}") String keySecret) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.keyId = keyId == null ? "" : keyId.trim();
        this.keySecret = keySecret == null ? "" : keySecret.trim();
    }

    @Override
    public String createOrder(long amountInSubunits, String currency, String receipt) {
        requireCredentials();
        Map<?, ?> response;
        try {
            response = restClient.post()
                .uri("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .headers(headers -> headers.setBasicAuth(keyId, keySecret))
                .body(Map.of(
                        "amount", amountInSubunits,
                        "currency", currency,
                        "receipt", receipt
                ))
                .retrieve()
                .body(Map.class);
        } catch (RuntimeException ex) {
            throw new IllegalStateException("Razorpay order creation failed", ex);
        }
        Object orderId = response == null ? null : response.get("id");
        if (!(orderId instanceof String id) || id.isBlank()) {
            throw new IllegalStateException("Razorpay did not return an order ID");
        }
        return id;
    }

    @Override
    public String keyId() {
        return keyId;
    }

    @Override
    public RazorpayPaymentDetails fetchPayment(String paymentId) {
        requireCredentials();
        Map<?, ?> response;
        try {
            response = restClient.get()
                    .uri("/payments/{paymentId}", paymentId)
                    .headers(headers -> headers.setBasicAuth(keyId, keySecret))
                    .retrieve()
                    .body(Map.class);
        } catch (RuntimeException ex) {
            throw new IllegalStateException("Razorpay payment lookup failed", ex);
        }
        if (response == null) {
            throw new IllegalStateException("Razorpay did not return payment details");
        }
        return new RazorpayPaymentDetails(
                stringValue(response, "id"),
                stringValue(response, "order_id"),
                longValue(response, "amount"),
                stringValue(response, "currency"),
                stringValue(response, "status"),
                Boolean.TRUE.equals(response.get("captured"))
        );
    }

    @Override
    public RazorpayRefundDetails createRefund(String paymentId, long amountInSubunits, String reference) {
        requireCredentials();
        Map<?, ?> response;
        try {
            response = restClient.post()
                    .uri("/payments/{paymentId}/refund", paymentId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> headers.setBasicAuth(keyId, keySecret))
                    .body(Map.of(
                            "amount", amountInSubunits,
                            "notes", Map.of("farmora_refund_request_id", reference)
                    ))
                    .retrieve()
                    .body(Map.class);
        } catch (RuntimeException ex) {
            throw new IllegalStateException("Razorpay refund creation failed; verify the refund in Razorpay before retrying", ex);
        }
        String refundId = response == null ? null : stringValue(response, "id");
        String status = response == null ? null : stringValue(response, "status");
        if (refundId == null || refundId.isBlank() || status == null || status.isBlank()) {
            throw new IllegalStateException("Razorpay did not return refund identity and status");
        }
        return new RazorpayRefundDetails(refundId, status);
    }

    private String stringValue(Map<?, ?> response, String name) {
        Object value = response.get(name);
        return value instanceof String text ? text : null;
    }

    private long longValue(Map<?, ?> response, String name) {
        Object value = response.get(name);
        if (value instanceof Number number) {
            return number.longValue();
        }
        return -1;
    }

    private void requireCredentials() {
        if (keyId.isBlank() || keySecret.isBlank()) {
            throw new IllegalStateException("Razorpay key ID and key secret are not configured");
        }
    }
}
