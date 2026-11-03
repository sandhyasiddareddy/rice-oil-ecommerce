package com.riceoil.platform.shipping.application;

import com.riceoil.platform.orders.domain.OrderEntity;
import com.riceoil.platform.orders.domain.OrderRepository;
import com.riceoil.platform.shipping.domain.ShipmentEntity;
import com.riceoil.platform.shipping.domain.ShipmentRepository;
import com.riceoil.platform.shipping.domain.ShipmentStatus;
import com.riceoil.platform.shipping.domain.ShipmentStatusEventEntity;
import com.riceoil.platform.shipping.domain.ShipmentStatusEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShipmentTrackingServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private ShipmentStatusEventRepository eventRepository;

    @Test
    void createShipment_shouldRecordInitialStatusEvent() throws Exception {
        OrderEntity order = new OrderEntity();
        setField(order, "id", 5L);
        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));
        when(shipmentRepository.findByShipmentCode("SR-5")).thenReturn(Optional.empty());
        when(shipmentRepository.save(any(ShipmentEntity.class))).thenAnswer(invocation -> {
            ShipmentEntity shipment = invocation.getArgument(0);
            setField(shipment, "id", 9L);
            return shipment;
        });
        when(eventRepository.save(any(ShipmentStatusEventEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventRepository.findByShipmentIdOrderByOccurredAtDesc(9L)).thenReturn(List.of());

        ShipmentSnapshot snapshot = service().createShipment(5L, "SR-5", "Shiprocket", "AWB5", "https://carrier.example/track/AWB5");

        assertEquals(ShipmentStatus.SHIPMENT_CREATED, snapshot.status());
        assertEquals("SHIPROCKET", snapshot.provider());
        verify(eventRepository).save(any(ShipmentStatusEventEntity.class));
    }

    @Test
    void updateStatus_shouldPersistTimelineEvent() throws Exception {
        OrderEntity order = new OrderEntity();
        setField(order, "id", 5L);
        ShipmentEntity shipment = new ShipmentEntity();
        setField(shipment, "id", 9L);
        shipment.setOrder(order);
        shipment.setShipmentCode("SR-5");
        shipment.setProvider("SHIPROCKET");
        shipment.setStatus(ShipmentStatus.PICKED_UP);
        when(shipmentRepository.findById(9L)).thenReturn(Optional.of(shipment));
        when(shipmentRepository.save(any(ShipmentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventRepository.save(any(ShipmentStatusEventEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventRepository.findByShipmentIdOrderByOccurredAtDesc(9L)).thenReturn(List.of());

        ShipmentSnapshot snapshot = service().updateStatus(9L, ShipmentStatus.IN_TRANSIT, "Package moving", null);

        assertEquals(ShipmentStatus.IN_TRANSIT, snapshot.status());
        verify(eventRepository).save(any(ShipmentStatusEventEntity.class));
    }

    private ShipmentTrackingService service() {
        return new ShipmentTrackingService(orderRepository, shipmentRepository, eventRepository);
    }

    private static void setField(Object target, String fieldName, Long value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
