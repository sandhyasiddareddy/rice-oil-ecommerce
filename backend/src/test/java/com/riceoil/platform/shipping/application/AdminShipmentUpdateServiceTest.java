package com.riceoil.platform.shipping.application;

import com.riceoil.platform.notifications.EmailNotificationSender;
import com.riceoil.platform.orders.domain.OrderEntity;
import com.riceoil.platform.orders.domain.OrderRepository;
import com.riceoil.platform.orders.domain.OrderStatus;
import com.riceoil.platform.orders.domain.aftercare.OrderCancellationRequestRepository;
import com.riceoil.platform.shipping.domain.ShipmentStatus;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminShipmentUpdateServiceTest {

    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final OrderCancellationRequestRepository cancellationRepository = mock(OrderCancellationRequestRepository.class);
    private final ShipmentTrackingService trackingService = mock(ShipmentTrackingService.class);
    private final EmailNotificationSender emailSender = mock(EmailNotificationSender.class);
    private final AdminShipmentUpdateService service = new AdminShipmentUpdateService(
            orderRepository, cancellationRepository, trackingService, emailSender,
            "https://shop.example", "5-7 working days");

    private OrderEntity order(OrderStatus status) throws Exception {
        OrderEntity order = new OrderEntity();
        Field id = OrderEntity.class.getDeclaredField("id");
        id.setAccessible(true);
        id.set(order, 9L);
        order.setOrderNumber("ORD-1");
        order.setStatus(status);
        order.setCustomerEmail("buyer@example.com");
        when(orderRepository.findByOrderNumber("ORD-1")).thenReturn(Optional.of(order));
        return order;
    }

    private ShipmentSnapshot snapshot(ShipmentStatus status) {
        return new ShipmentSnapshot(4L, 9L, "SH-ORD-1", "DTDC", "TRK1", null, status, List.of());
    }

    @Test
    void createsShipmentOnFirstUpdateAndEmailsCustomer() throws Exception {
        order(OrderStatus.PAID);
        when(trackingService.getOrderShipments(9L)).thenReturn(List.of());
        when(trackingService.createShipment(eq(9L), eq("SH-ORD-1"), eq("DTDC"), eq("TRK1"), isNull()))
                .thenReturn(snapshot(ShipmentStatus.SHIPMENT_CREATED));
        when(trackingService.updateStatus(eq(4L), eq(ShipmentStatus.IN_TRANSIT), any(), any()))
                .thenReturn(snapshot(ShipmentStatus.IN_TRANSIT));

        service.update("ORD-1", "DTDC", "TRK1", null, ShipmentStatus.IN_TRANSIT, null);

        verify(emailSender).sendOrderUpdate(eq("buyer@example.com"), anyString(), anyString());
    }

    @Test
    void updatesExistingShipmentTracking() throws Exception {
        order(OrderStatus.PAID);
        when(trackingService.getOrderShipments(9L)).thenReturn(List.of(snapshot(ShipmentStatus.IN_TRANSIT)));
        when(trackingService.updateTracking(4L, null, "TRK2", null)).thenReturn(snapshot(ShipmentStatus.IN_TRANSIT));

        service.update("ORD-1", null, "TRK2", null, ShipmentStatus.IN_TRANSIT, null);

        verify(trackingService).updateTracking(4L, null, "TRK2", null);
        verify(trackingService, never()).createShipment(any(), any(), any(), any(), any());
        verify(trackingService, never()).updateStatus(any(), any(), any(), any());
        verify(emailSender, never()).sendOrderUpdate(any(), any(), any());
    }

    @Test
    void rejectsUpdatesWhileCancellationIsPending() throws Exception {
        order(OrderStatus.PAID);
        when(cancellationRepository.existsByOrderIdAndStatusIn(eq(9L), any())).thenReturn(true);

        assertThrows(IllegalStateException.class,
                () -> service.update("ORD-1", null, "TRK1", null, ShipmentStatus.PACKED, null));
        verify(trackingService, never()).createShipment(any(), any(), any(), any(), any());
    }

    @Test
    void hidesPaidOrdersWithOpenCancellationFromFulfilmentList() throws Exception {
        OrderEntity open = order(OrderStatus.PAID);
        when(orderRepository.findTop200ByStatusOrderByIdDesc(OrderStatus.PAID)).thenReturn(List.of(open));
        when(cancellationRepository.existsByOrderIdAndStatusIn(eq(9L), any())).thenReturn(true);

        org.junit.jupiter.api.Assertions.assertTrue(service.listPaidOrders().isEmpty());
    }

    @Test
    void rejectsUnpaidOrders() throws Exception {
        order(OrderStatus.CREATED);

        assertThrows(IllegalArgumentException.class,
                () -> service.update("ORD-1", null, "TRK1", null, ShipmentStatus.IN_TRANSIT, null));
        verify(trackingService, never()).createShipment(any(), any(), any(), any(), any());
    }
}
