package com.riceoil.platform.orders.application.aftercare;

import com.riceoil.platform.orders.application.GuestOrderAccessService;
import com.riceoil.platform.orders.domain.OrderEntity;
import com.riceoil.platform.orders.domain.OrderRepository;
import com.riceoil.platform.orders.domain.OrderStatus;
import com.riceoil.platform.orders.domain.aftercare.OrderCancellationRequestRepository;
import com.riceoil.platform.orders.domain.aftercare.RefundRequestEntity;
import com.riceoil.platform.orders.domain.aftercare.RefundRequestRepository;
import com.riceoil.platform.orders.domain.aftercare.RequestStatus;
import com.riceoil.platform.shipping.domain.ShipmentEntity;
import com.riceoil.platform.shipping.domain.ShipmentRepository;
import com.riceoil.platform.shipping.domain.ShipmentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AftercareServiceTest {

    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final OrderCancellationRequestRepository cancellationRepository =
            mock(OrderCancellationRequestRepository.class);
    private final RefundRequestRepository refundRepository = mock(RefundRequestRepository.class);
    private final GuestOrderAccessService guestOrderAccessService = mock(GuestOrderAccessService.class);
    private final ShipmentRepository shipmentRepository = mock(ShipmentRepository.class);
    private final AftercareService service = new AftercareService(
            orderRepository, cancellationRepository, refundRepository, guestOrderAccessService, shipmentRepository);

    @BeforeEach
    void setUp() throws Exception {
        OrderEntity order = paidOrder();
        when(orderRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(order));
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(shipmentRepository.findByOrderIdOrderByCreatedAtAsc(10L)).thenReturn(List.of());
        when(cancellationRepository.existsByOrderIdAndStatusIn(10L,
                List.of(RequestStatus.REQUESTED, RequestStatus.PROCESSING))).thenReturn(false);
        when(refundRepository.save(any(RefundRequestEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void requestCancellation_shouldRejectDeliveredOrder() {
        ShipmentEntity shipment = new ShipmentEntity();
        shipment.setStatus(ShipmentStatus.DELIVERED);
        when(shipmentRepository.findByOrderIdOrderByCreatedAtAsc(10L)).thenReturn(List.of(shipment));

        assertThrows(IllegalStateException.class,
                () -> service.requestCancellation(10L, null, 25L, null));
    }

    @Test
    void requestCancellation_shouldRequireOrderOwner() {
        assertThrows(AccessDeniedException.class,
                () -> service.requestCancellation(10L, null, 99L, null));
    }

    @Test
    void requestRefund_shouldNotExceedRemainingCapturedAmount() {
        RefundRequestEntity previousRefund = new RefundRequestEntity();
        previousRefund.setAmount(new BigDecimal("75.00"));
        previousRefund.setStatus(RequestStatus.PROCESSED);
        when(refundRepository.findByOrderIdOrderByIdDesc(10L)).thenReturn(List.of(previousRefund));

        assertThrows(IllegalArgumentException.class,
                () -> service.requestRefund(10L, new BigDecimal("116.00"), null, 25L, null));
    }

    @Test
    void requestRefund_shouldPersistRequestWithoutCallingProvider() {
        when(refundRepository.findByOrderIdOrderByIdDesc(10L)).thenReturn(List.of());

        RefundSnapshot snapshot = service.requestRefund(
                10L, new BigDecimal("40.00"), "Damaged", 25L, null);

        assertEquals("REQUESTED", snapshot.status());
        assertEquals(new BigDecimal("40.00"), snapshot.amount());
    }

    private OrderEntity paidOrder() throws Exception {
        OrderEntity order = new OrderEntity();
        var idField = OrderEntity.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(order, 10L);
        order.setCustomerId(25L);
        order.setStatus(OrderStatus.PAID);
        order.setPaymentProvider("RAZORPAY");
        order.setPaymentId("pay_123");
        order.setTotalAmount(new BigDecimal("190.00"));
        order.setCurrency("INR");
        return order;
    }
}
