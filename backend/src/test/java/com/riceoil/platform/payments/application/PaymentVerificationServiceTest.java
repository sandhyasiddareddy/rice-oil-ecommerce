package com.riceoil.platform.payments.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.riceoil.platform.orders.application.GuestOrderAccessService;
import com.riceoil.platform.orders.application.GuestOrderAccessSnapshot;
import com.riceoil.platform.orders.domain.OrderEntity;
import com.riceoil.platform.orders.domain.OrderRepository;
import com.riceoil.platform.orders.domain.OrderStatus;
import com.riceoil.platform.payments.domain.PaymentWebhookEventEntity;
import com.riceoil.platform.payments.domain.PaymentWebhookEventRepository;
import com.riceoil.platform.orders.domain.aftercare.RefundRequestEntity;
import com.riceoil.platform.orders.domain.aftercare.RefundRequestRepository;
import com.riceoil.platform.orders.domain.aftercare.RequestStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentVerificationServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentWebhookEventRepository webhookEventRepository;

    @Mock
    private RefundRequestRepository refundRequestRepository;

    @Mock
    private GuestOrderAccessService guestOrderAccessService;

    @Mock
    private com.riceoil.platform.orders.application.OrderFulfilmentService orderFulfilmentService;

    @Mock
    private RazorpayGateway razorpayGateway;

    private PaymentVerificationService service;

    @BeforeEach
    void setUp() {
        service = new PaymentVerificationService(
                orderRepository,
                webhookEventRepository,
                refundRequestRepository,
                guestOrderAccessService,
                orderFulfilmentService,
                razorpayGateway,
                "key-secret",
                "webhook-secret",
                new ObjectMapper()
        );
    }

    @Test
    void verify_shouldKeepProviderOrderReferenceAndLinkPaymentToOwnedOrder() throws Exception {
        OrderEntity order = paymentPendingOrder();
        when(orderRepository.findById(15L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(razorpayGateway.fetchPayment("pay_123")).thenReturn(new RazorpayPaymentDetails(
                "pay_123", "rpay_gateway_order", 19000L, "INR", "captured", true
        ));
        String signature = hmac("rpay_gateway_order|pay_123", "key-secret");

        service.verify(15L, "pay_123", signature, 42L, null);

        assertEquals("rpay_gateway_order", order.getPaymentReference());
        assertEquals("pay_123", order.getPaymentId());
        assertEquals(OrderStatus.PAID, order.getStatus());
        verify(orderRepository).save(order);
    }

    @Test
    void verify_shouldRejectAnOrderOwnedByAnotherCustomer() throws Exception {
        when(orderRepository.findById(15L)).thenReturn(Optional.of(paymentPendingOrder()));

        assertThrows(AccessDeniedException.class,
                () -> service.verify(15L, "pay_123", "signature", 99L, null));
    }

    @Test
    void verify_shouldAllowGuestWithItsOrderAccessToken() throws Exception {
        OrderEntity order = paymentPendingOrder();
        order.setCustomerId(null);
        when(orderRepository.findById(15L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(guestOrderAccessService.verifyAndGetOrder(15L, "guest-token"))
                .thenReturn(new GuestOrderAccessSnapshot(15L, "ORD-15", OrderStatus.PAYMENT_PENDING,
                        "INR", new java.math.BigDecimal("190.00")));
        when(razorpayGateway.fetchPayment("pay_123")).thenReturn(new RazorpayPaymentDetails(
                "pay_123", "rpay_gateway_order", 19000L, "INR", "captured", true
        ));
        String signature = hmac("rpay_gateway_order|pay_123", "key-secret");

        service.verify(15L, "pay_123", signature, null, "guest-token");

        assertEquals(OrderStatus.PAID, order.getStatus());
        verify(guestOrderAccessService).verifyAndGetOrder(15L, "guest-token");
    }

    @Test
    void processWebhook_shouldVerifyRawBodyAndUpdateTheMatchingOrder() throws Exception {
        OrderEntity order = paymentPendingOrder();
        String payload = "{\"id\":\"evt_1\",\"event\":\"payment.captured\","
                + "\"payload\":{\"payment\":{\"entity\":{\"id\":\"pay_123\","
                + "\"order_id\":\"rpay_gateway_order\",\"amount\":19000,\"currency\":\"INR\"}}}}";
        when(webhookEventRepository.findByEventId("evt_1")).thenReturn(Optional.empty());
        when(orderRepository.findByPaymentReference("rpay_gateway_order")).thenReturn(Optional.of(order));
        when(webhookEventRepository.save(any(PaymentWebhookEventEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.processWebhook(payload, hmac(payload, "webhook-secret"));

        assertEquals(OrderStatus.PAID, order.getStatus());
        assertEquals("rpay_gateway_order", order.getPaymentReference());
        assertEquals("pay_123", order.getPaymentId());
        verify(orderRepository).save(order);
    }

    @Test
    void processWebhook_shouldRefundAPaymentThatArrivesAfterTheOrderExpired() throws Exception {
        OrderEntity order = paymentPendingOrder();
        order.setStatus(OrderStatus.CANCELLED);
        order.setOrderNumber("ORD-15");
        String payload = "{\"id\":\"evt_2\",\"event\":\"payment.captured\","
                + "\"payload\":{\"payment\":{\"entity\":{\"id\":\"pay_123\","
                + "\"order_id\":\"rpay_gateway_order\",\"amount\":19000,\"currency\":\"INR\"}}}}";
        when(webhookEventRepository.findByEventId("evt_2")).thenReturn(Optional.empty());
        when(orderRepository.findByPaymentReference("rpay_gateway_order")).thenReturn(Optional.of(order));
        when(webhookEventRepository.save(any(PaymentWebhookEventEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(razorpayGateway.createRefund("pay_123", 19000L, "late-ORD-15"))
                .thenReturn(new RazorpayRefundDetails("rfnd_1", "pending"));

        service.processWebhook(payload, hmac(payload, "webhook-secret"));

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        var captor = org.mockito.ArgumentCaptor.forClass(RefundRequestEntity.class);
        verify(refundRequestRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        RefundRequestEntity saved = captor.getValue();
        assertEquals("rfnd_1", saved.getProviderRefundId());
        assertEquals(RequestStatus.PENDING, saved.getStatus());
        verify(orderFulfilmentService, org.mockito.Mockito.never()).onPaid(any());
    }

    @Test
    void verify_shouldRefundAndReportCancelledWhenOrderAlreadyExpired() throws Exception {
        OrderEntity order = paymentPendingOrder();
        order.setStatus(OrderStatus.CANCELLED);
        order.setOrderNumber("ORD-15");
        when(orderRepository.findById(15L)).thenReturn(Optional.of(order));
        when(razorpayGateway.fetchPayment("pay_123")).thenReturn(new RazorpayPaymentDetails(
                "pay_123", "rpay_gateway_order", 19000L, "INR", "captured", true
        ));
        when(razorpayGateway.createRefund("pay_123", 19000L, "late-ORD-15"))
                .thenReturn(new RazorpayRefundDetails("rfnd_1", "processed"));

        RazorpayVerifyResult result = service.verify(15L, "pay_123", hmac("rpay_gateway_order|pay_123", "key-secret"), 42L, null);

        assertEquals(OrderStatus.CANCELLED, result.status());
        assertEquals(false, result.verified());
        verify(razorpayGateway).createRefund("pay_123", 19000L, "late-ORD-15");
    }

    @Test
    void processWebhook_shouldRejectInvalidSignature() {
        assertThrows(IllegalArgumentException.class,
                () -> service.processWebhook("{\"id\":\"evt_1\"}", "not-a-signature"));
    }

    private OrderEntity paymentPendingOrder() throws Exception {
        OrderEntity order = new OrderEntity();
        var idField = OrderEntity.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(order, 15L);
        order.setCustomerId(42L);
        order.setPaymentProvider("RAZORPAY");
        order.setPaymentReference("rpay_gateway_order");
        order.setStatus(OrderStatus.PAYMENT_PENDING);
        order.setCurrency("INR");
        order.setTotalAmount(new java.math.BigDecimal("190.00"));
        return order;
    }

    private String hmac(String payload, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }
}
