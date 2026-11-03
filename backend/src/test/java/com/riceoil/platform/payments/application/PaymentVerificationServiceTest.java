package com.riceoil.platform.payments.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.riceoil.platform.orders.application.GuestOrderAccessService;
import com.riceoil.platform.orders.application.GuestOrderAccessSnapshot;
import com.riceoil.platform.orders.domain.OrderEntity;
import com.riceoil.platform.orders.domain.OrderRepository;
import com.riceoil.platform.orders.domain.OrderStatus;
import com.riceoil.platform.payments.domain.PaymentWebhookEventEntity;
import com.riceoil.platform.payments.domain.PaymentWebhookEventRepository;
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
    private GuestOrderAccessService guestOrderAccessService;

    private PaymentVerificationService service;

    @BeforeEach
    void setUp() {
        service = new PaymentVerificationService(
                orderRepository,
                webhookEventRepository,
                guestOrderAccessService,
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
                .thenReturn(new GuestOrderAccessSnapshot(15L, "ORD-15", OrderStatus.PAYMENT_PENDING));
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
                + "\"order_id\":\"rpay_gateway_order\"}}}}";
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
        return order;
    }

    private String hmac(String payload, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }
}
