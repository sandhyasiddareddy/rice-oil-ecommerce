package com.riceoil.platform.payments.application;

import com.riceoil.platform.orders.domain.OrderEntity;
import com.riceoil.platform.orders.domain.OrderRepository;
import com.riceoil.platform.orders.domain.OrderStatus;
import com.riceoil.platform.orders.application.GuestOrderAccessService;
import com.riceoil.platform.orders.application.GuestOrderAccessSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentOrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private GuestOrderAccessService guestOrderAccessService;

    private PaymentOrderService paymentOrderService;

    @BeforeEach
    void setUp() {
        this.paymentOrderService = new PaymentOrderService(orderRepository, guestOrderAccessService);
    }

    @Test
    void createRazorpayOrder_shouldLinkPaymentOnce() throws Exception {
        OrderEntity order = new OrderEntity();
        setPrivateId(order, 5L);
        order.setCustomerId(9L);
        order.setCurrency("INR");
        order.setTotalAmount(new BigDecimal("190.00"));
        order.setStatus(OrderStatus.CREATED);

        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RazorpayCreateOrderResult first = paymentOrderService.createRazorpayOrder(5L, 9L, null);
        RazorpayCreateOrderResult second = paymentOrderService.createRazorpayOrder(5L, 9L, null);

        assertTrue(first.providerOrderId().startsWith("rpay_"));
        assertEquals(first.providerOrderId(), second.providerOrderId());
        assertEquals(OrderStatus.PAYMENT_PENDING, second.status());
    }

    @Test
    void createRazorpayOrder_shouldRejectAnotherCustomersOrder() throws Exception {
        OrderEntity order = new OrderEntity();
        setPrivateId(order, 5L);
        order.setCustomerId(9L);
        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> paymentOrderService.createRazorpayOrder(5L, 10L, null));
    }

    @Test
    void createRazorpayOrder_shouldAllowGuestWithItsOrderAccessToken() throws Exception {
        OrderEntity order = new OrderEntity();
        setPrivateId(order, 5L);
        order.setCurrency("INR");
        order.setTotalAmount(new BigDecimal("190.00"));
        order.setStatus(OrderStatus.CREATED);
        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(guestOrderAccessService.verifyAndGetOrder(5L, "guest-token"))
                .thenReturn(new GuestOrderAccessSnapshot(5L, "ORD-5", OrderStatus.CREATED));

        RazorpayCreateOrderResult result = paymentOrderService.createRazorpayOrder(5L, null, "guest-token");

        assertEquals(OrderStatus.PAYMENT_PENDING, result.status());
        verify(guestOrderAccessService).verifyAndGetOrder(5L, "guest-token");
    }

    private void setPrivateId(Object target, Long id) throws Exception {
        var idField = target.getClass().getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(target, id);
    }
}
