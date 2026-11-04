package com.riceoil.platform.payments.application;

import com.riceoil.platform.orders.domain.OrderEntity;
import com.riceoil.platform.orders.domain.OrderRepository;
import com.riceoil.platform.orders.domain.OrderStatus;
import com.riceoil.platform.orders.application.GuestOrderAccessService;
import com.riceoil.platform.orders.application.OrderFulfilmentService;
import com.riceoil.platform.payments.domain.PaymentWebhookEventEntity;
import com.riceoil.platform.payments.domain.PaymentWebhookEventRepository;
import com.riceoil.platform.orders.domain.aftercare.RefundRequestEntity;
import com.riceoil.platform.orders.domain.aftercare.RefundRequestRepository;
import com.riceoil.platform.orders.domain.aftercare.RequestStatus;
import com.riceoil.platform.shared.observability.BusinessLog;
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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

@Service
public class PaymentVerificationService {

    private static final String RAZORPAY = "RAZORPAY";
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PaymentVerificationService.class);

    private final OrderRepository orderRepository;
    private final PaymentWebhookEventRepository webhookEventRepository;
    private final RefundRequestRepository refundRequestRepository;
    private final GuestOrderAccessService guestOrderAccessService;
    private final OrderFulfilmentService orderFulfilmentService;
    private final RazorpayGateway razorpayGateway;
    private final String razorpayKeySecret;
    private final String razorpayWebhookSecret;
    private final ObjectMapper objectMapper;

    public PaymentVerificationService(OrderRepository orderRepository,
                                      PaymentWebhookEventRepository webhookEventRepository,
                                      RefundRequestRepository refundRequestRepository,
                                      GuestOrderAccessService guestOrderAccessService,
                                      OrderFulfilmentService orderFulfilmentService,
                                      RazorpayGateway razorpayGateway,
                                      @Value("${app.payments.razorpay.key-secret:}") String razorpayKeySecret,
                                      @Value("${app.payments.razorpay.webhook-secret:}") String razorpayWebhookSecret,
                                      ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.webhookEventRepository = webhookEventRepository;
        this.refundRequestRepository = refundRequestRepository;
        this.guestOrderAccessService = guestOrderAccessService;
        this.orderFulfilmentService = orderFulfilmentService;
        this.razorpayGateway = razorpayGateway;
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
            BusinessLog.warn("PAYMENT_VERIFY_REJECTED", "orderNumber", order.getOrderNumber(),
                    "razorpayOrderId", order.getPaymentReference(), "razorpayPaymentId", paymentId,
                    "reason", "invalid_signature");
            throw new IllegalArgumentException("Invalid payment signature");
        }
        if (order.getStatus() == OrderStatus.PAID && Objects.equals(order.getPaymentId(), paymentId)) {
            BusinessLog.info("PAYMENT_VERIFY_ALREADY_PAID", "orderNumber", order.getOrderNumber(),
                    "razorpayPaymentId", paymentId);
            return new RazorpayVerifyResult(order.getId(), paymentId, order.getStatus(), true);
        }
        boolean expired = order.getStatus() == OrderStatus.CANCELLED;
        if (!expired && order.getStatus() != OrderStatus.PAYMENT_PENDING) {
            BusinessLog.warn("PAYMENT_VERIFY_REJECTED", "orderNumber", order.getOrderNumber(),
                    "razorpayPaymentId", paymentId, "reason", "order_not_awaiting_payment",
                    "orderStatus", order.getStatus());
            throw new IllegalStateException("Order is not awaiting payment");
        }
        RazorpayPaymentDetails payment = razorpayGateway.fetchPayment(paymentId);
        if (!paymentId.equals(payment.id())
                || !order.getPaymentReference().equals(payment.orderId())
                || payment.amountInSubunits() != toSubunits(order.getTotalAmount())
                || !order.getCurrency().equalsIgnoreCase(payment.currency())
                || !payment.captured()
                || !"captured".equalsIgnoreCase(payment.status())) {
            BusinessLog.warn("PAYMENT_VERIFY_REJECTED", "orderNumber", order.getOrderNumber(),
                    "razorpayOrderId", order.getPaymentReference(), "razorpayPaymentId", paymentId,
                    "reason", "payment_mismatch_or_not_captured",
                    "expectedAmountSubunits", toSubunits(order.getTotalAmount()),
                    "providerAmountSubunits", payment.amountInSubunits(),
                    "providerStatus", payment.status(), "providerCaptured", payment.captured());
            throw new IllegalArgumentException("Razorpay payment does not match the order or is not captured");
        }

        if (expired) {
            refundLatePayment(order, paymentId);
            return new RazorpayVerifyResult(order.getId(), paymentId, order.getStatus(), false);
        }

        order.setStatus(OrderStatus.PAID);
        order.setPaymentProvider(RAZORPAY);
        order.setPaymentId(paymentId);
        order.setPaymentLinkedAt(java.time.OffsetDateTime.now());
        orderRepository.save(order);
        BusinessLog.info("PAYMENT_VERIFIED", "orderNumber", order.getOrderNumber(),
                "razorpayOrderId", order.getPaymentReference(), "razorpayPaymentId", paymentId,
                "amountSubunits", payment.amountInSubunits(), "source", "checkout_callback");
        orderFulfilmentService.onPaid(order);

        return new RazorpayVerifyResult(order.getId(), paymentId, order.getStatus(), true);
    }

    @Transactional
    public void processWebhook(String payload, String signature) {
        if (razorpayWebhookSecret.isBlank()) {
            throw new IllegalStateException("Razorpay webhook secret is not configured");
        }
        if (!isValidSignature(payload, signature, razorpayWebhookSecret)) {
            BusinessLog.warn("WEBHOOK_REJECTED", "reason", "invalid_signature");
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
            BusinessLog.info("WEBHOOK_DUPLICATE_IGNORED", "eventId", eventId, "type", eventType);
            return;
        }

        JsonNode payment = root.path("payload").path("payment").path("entity");
        String paymentId = payment.path("id").asText(null);
        String providerOrderId = payment.path("order_id").asText(null);
        BusinessLog.info("WEBHOOK_RECEIVED", "eventId", eventId, "type", eventType,
                "razorpayPaymentId", paymentId, "razorpayOrderId", providerOrderId);

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
            long amountInSubunits = payment.path("amount").asLong(-1);
            String currency = payment.path("currency").asText(null);
            if (amountInSubunits != toSubunits(order.getTotalAmount())
                    || currency == null
                    || !order.getCurrency().equalsIgnoreCase(currency)) {
                BusinessLog.warn("WEBHOOK_PAYMENT_MISMATCH", "eventId", eventId, "orderNumber", order.getOrderNumber(),
                        "razorpayPaymentId", paymentId, "expectedAmountSubunits", toSubunits(order.getTotalAmount()),
                        "providerAmountSubunits", amountInSubunits, "providerCurrency", currency);
                throw new IllegalArgumentException("Captured payment amount or currency does not match the order");
            }
            if (order.getStatus() == OrderStatus.CANCELLED) {
                refundLatePayment(order, paymentId);
                event.setProcessed(true);
                webhookEventRepository.save(event);
                return;
            }
            if (order.getStatus() != OrderStatus.PAYMENT_PENDING
                    && !(order.getStatus() == OrderStatus.PAID && Objects.equals(order.getPaymentId(), paymentId))) {
                BusinessLog.warn("WEBHOOK_REJECTED", "eventId", eventId, "orderNumber", order.getOrderNumber(),
                        "razorpayPaymentId", paymentId, "reason", "order_not_awaiting_payment",
                        "orderStatus", order.getStatus());
                throw new IllegalStateException("Order is not awaiting this payment");
            }
            boolean newlyPaid = order.getStatus() != OrderStatus.PAID;
            order.setStatus(OrderStatus.PAID);
            order.setPaymentProvider(RAZORPAY);
            order.setPaymentId(paymentId);
            order.setPaymentLinkedAt(java.time.OffsetDateTime.now());
            orderRepository.save(order);
            BusinessLog.info("PAYMENT_VERIFIED", "orderNumber", order.getOrderNumber(),
                    "razorpayOrderId", providerOrderId, "razorpayPaymentId", paymentId,
                    "amountSubunits", amountInSubunits, "source", "webhook", "newlyPaid", newlyPaid);
            if (newlyPaid) {
                orderFulfilmentService.onPaid(order);
            }
        } else if (eventType.startsWith("refund.")) {
            JsonNode refund = root.path("payload").path("refund").path("entity");
            String refundId = refund.path("id").asText(null);
            if (isBlank(refundId)) {
                throw new IllegalArgumentException("Refund webhook is missing refund identity");
            }
            RefundRequestEntity refundRequest = refundRequestRepository.findByProviderRefundId(refundId)
                    .orElseThrow(() -> new ResourceNotFoundException("Refund request not found for Razorpay refund"));
            String refundStatus = refund.path("status").asText(eventType.substring("refund.".length()));
            refundRequest.setProviderStatus(refundStatus);
            refundRequest.setStatus(switch (refundStatus.toLowerCase(java.util.Locale.ROOT)) {
                case "processed" -> RequestStatus.PROCESSED;
                case "failed" -> RequestStatus.FAILED;
                default -> RequestStatus.PENDING;
            });
            refundRequestRepository.save(refundRequest);
            BusinessLog.info("REFUND_STATUS_UPDATED", "eventId", eventId, "refundRequestId", refundRequest.getId(),
                    "orderNumber", refundRequest.getOrder().getOrderNumber(), "razorpayRefundId", refundId,
                    "providerStatus", refundStatus, "status", refundRequest.getStatus(),
                    "amount", refundRequest.getAmount());
        }
        event.setProcessed(true);
        webhookEventRepository.save(event);
    }

    /** A captured payment arrived for an order that already expired: return the money automatically. */
    private void refundLatePayment(OrderEntity order, String paymentId) {
        if (refundRequestRepository.existsByOrderIdAndStatusIn(order.getId(), List.of(
                RequestStatus.REQUESTED, RequestStatus.PROCESSING, RequestStatus.PENDING,
                RequestStatus.PROCESSED, RequestStatus.APPROVED))) {
            return;
        }
        order.setPaymentId(paymentId);
        orderRepository.save(order);

        RefundRequestEntity refund = new RefundRequestEntity();
        refund.setOrder(order);
        refund.setAmount(order.getTotalAmount());
        refund.setReason("Payment received after the order expired");
        refund.setStatus(RequestStatus.PROCESSING);
        refundRequestRepository.save(refund);
        BusinessLog.info("LATE_PAYMENT_REFUND_STARTED", "orderNumber", order.getOrderNumber(),
                "razorpayPaymentId", paymentId, "amount", order.getTotalAmount());
        try {
            RazorpayRefundDetails result = razorpayGateway.createRefund(
                    paymentId, toSubunits(order.getTotalAmount()), "late-" + order.getOrderNumber());
            refund.setProviderRefundId(result.id());
            refund.setProviderStatus(result.status());
            refund.setStatus("processed".equalsIgnoreCase(result.status())
                    ? RequestStatus.PROCESSED : RequestStatus.PENDING);
            BusinessLog.info("LATE_PAYMENT_REFUND_SENT", "orderNumber", order.getOrderNumber(),
                    "razorpayPaymentId", paymentId, "razorpayRefundId", result.id(), "providerStatus", result.status());
        } catch (RuntimeException ex) {
            BusinessLog.error("LATE_PAYMENT_REFUND_FAILED", ex, "orderNumber", order.getOrderNumber(),
                    "razorpayPaymentId", paymentId, "reason", ex.getMessage());
            refund.setStatus(RequestStatus.FAILED);
        }
        refundRequestRepository.save(refund);
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

    private long toSubunits(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.UNNECESSARY)
                .longValueExact();
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
