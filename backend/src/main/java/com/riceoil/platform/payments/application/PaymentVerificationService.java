package com.riceoil.platform.payments.application;

import com.riceoil.platform.orders.domain.OrderEntity;
import com.riceoil.platform.orders.domain.OrderRepository;
import com.riceoil.platform.orders.domain.OrderStatus;
import com.riceoil.platform.orders.application.GuestOrderAccessService;
import com.riceoil.platform.payments.domain.PaymentWebhookEventEntity;
import com.riceoil.platform.payments.domain.PaymentWebhookEventRepository;
import com.riceoil.platform.shared.web.ResourceNotFoundException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Objects;

@Service
public class PaymentVerificationService {

    private static final String RAZORPAY = "RAZORPAY";

    private final OrderRepository orderRepository;
    private final PaymentWebhookEventRepository webhookEventRepository;
    private final GuestOrderAccessService guestOrderAccessService;
    private final String razorpayKeySecret;
    private final String razorpayWebhookSecret;
    private final ObjectMapper objectMapper;

    public PaymentVerificationService(OrderRepository orderRepository,
                                      PaymentWebhookEventRepository webhookEventRepository,
                                      GuestOrderAccessService guestOrderAccessService,
                                      @Value("${app.payments.razorpay.key-secret:}") String razorpayKeySecret,
                                      @Value("${app.payments.razorpay.webhook-secret:}") String razorpayWebhookSecret,
                                      ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.webhookEventRepository = webhookEventRepository;
        this.guestOrderAccessService = guestOrderAccessService;
        this.razorpayKeySecret = razorpayKeySecret == null ? "" : razorpayKeySecret.trim();
        this.razorpayWebhookSecret = razorpayWebhookSecret == null ? "" : razorpayWebhookSecret.trim();
        this.objectMapper = objectMapper;
    }

    @Transactional
    public RazorpayVerifyResult verify(Long orderId, String paymentId, String signature,
                                       Long customerId, String guestOrderToken) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        requireOwner(order, customerId, guestOrderToken);

        if (razorpayKeySecret.isBlank()) {
            throw new IllegalStateException("Razorpay key secret is not configured");
        }
        if (order.getPaymentReference() == null || order.getPaymentReference().isBlank()
                || !isValidSignature(order.getPaymentReference(), paymentId, signature, razorpayKeySecret)) {
            throw new IllegalArgumentException("Invalid payment signature");
        }
        if (order.getStatus() == OrderStatus.PAID && Objects.equals(order.getPaymentId(), paymentId)) {
            return new RazorpayVerifyResult(order.getId(), paymentId, order.getStatus(), true);
        }
        if (order.getStatus() != OrderStatus.PAYMENT_PENDING) {
            throw new IllegalStateException("Order is not awaiting payment");
        }

        order.setStatus(OrderStatus.PAID);
        order.setPaymentProvider(RAZORPAY);
        order.setPaymentId(paymentId);
        order.setPaymentLinkedAt(java.time.OffsetDateTime.now());
        orderRepository.save(order);

        return new RazorpayVerifyResult(order.getId(), paymentId, order.getStatus(), true);
    }

    @Transactional
    public void processWebhook(String payload, String signature) {
        if (razorpayWebhookSecret.isBlank()) {
            throw new IllegalStateException("Razorpay webhook secret is not configured");
        }
        if (!isValidSignature(payload, signature, razorpayWebhookSecret)) {
            throw new IllegalArgumentException("Invalid Razorpay webhook signature");
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(payload);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid Razorpay webhook payload", ex);
        }
        String eventId = root.path("id").asText(null);
        String eventType = root.path("event").asText(null);
        if (isBlank(eventId) || isBlank(eventType)) {
            throw new IllegalArgumentException("Razorpay webhook is missing event identity");
        }

        if (webhookEventRepository.findByEventId(eventId).isPresent()) {
            return;
        }

        JsonNode payment = root.path("payload").path("payment").path("entity");
        String paymentId = payment.path("id").asText(null);
        String providerOrderId = payment.path("order_id").asText(null);

        PaymentWebhookEventEntity event = new PaymentWebhookEventEntity();
        event.setEventId(eventId);
        event.setProvider(RAZORPAY);
        event.setPayloadJson(payload);
        event.setProcessed(false);
        webhookEventRepository.save(event);

        if ("payment.captured".equalsIgnoreCase(eventType)) {
            if (isBlank(paymentId) || isBlank(providerOrderId)) {
                throw new IllegalArgumentException("Captured payment webhook is missing payment identity");
            }
            OrderEntity order = orderRepository.findByPaymentReference(providerOrderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Order not found for Razorpay order"));
            if (order.getStatus() != OrderStatus.PAYMENT_PENDING
                    && !(order.getStatus() == OrderStatus.PAID && Objects.equals(order.getPaymentId(), paymentId))) {
                throw new IllegalStateException("Order is not awaiting this payment");
            }
            order.setStatus(OrderStatus.PAID);
            order.setPaymentProvider(RAZORPAY);
            order.setPaymentId(paymentId);
            order.setPaymentLinkedAt(java.time.OffsetDateTime.now());
            orderRepository.save(order);
        }
        event.setProcessed(true);
        webhookEventRepository.save(event);
    }

    private void requireOwner(OrderEntity order, Long customerId, String guestOrderToken) {
        if (order.getCustomerId() != null) {
            if (customerId == null || !Objects.equals(order.getCustomerId(), customerId)) {
                throw new AccessDeniedException("Order does not belong to the authenticated customer");
            }
            return;
        }
        if (customerId != null || isBlank(guestOrderToken)) {
            throw new AccessDeniedException("Guest order access token is required");
        }
        guestOrderAccessService.verifyAndGetOrder(order.getId(), guestOrderToken);
    }

    private boolean isValidSignature(String providerOrderId, String paymentId, String signature, String secret) {
        if (isBlank(providerOrderId) || isBlank(paymentId) || isBlank(signature)) {
            return false;
        }
        return isValidSignature(providerOrderId + "|" + paymentId, signature, secret);
    }

    private boolean isValidSignature(String payload, String signature, String secret) {
        if (isBlank(payload) || isBlank(signature) || isBlank(secret)) {
            return false;
        }
        String expected = hmacSha256(payload, secret);
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII),
                signature.getBytes(StandardCharsets.US_ASCII));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String hmacSha256(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] raw = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(raw);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to validate Razorpay signature", ex);
        }
    }
}
