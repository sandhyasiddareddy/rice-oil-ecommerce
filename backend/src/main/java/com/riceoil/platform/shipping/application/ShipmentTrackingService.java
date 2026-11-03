package com.riceoil.platform.shipping.application;

import com.riceoil.platform.orders.domain.OrderEntity;
import com.riceoil.platform.orders.domain.OrderRepository;
import com.riceoil.platform.shared.web.ResourceNotFoundException;
import com.riceoil.platform.shipping.domain.ShipmentEntity;
import com.riceoil.platform.shipping.domain.ShipmentRepository;
import com.riceoil.platform.shipping.domain.ShipmentStatus;
import com.riceoil.platform.shipping.domain.ShipmentStatusEventEntity;
import com.riceoil.platform.shipping.domain.ShipmentStatusEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class ShipmentTrackingService {

    private final OrderRepository orderRepository;
    private final ShipmentRepository shipmentRepository;
    private final ShipmentStatusEventRepository eventRepository;

    public ShipmentTrackingService(OrderRepository orderRepository,
                                   ShipmentRepository shipmentRepository,
                                   ShipmentStatusEventRepository eventRepository) {
        this.orderRepository = orderRepository;
        this.shipmentRepository = shipmentRepository;
        this.eventRepository = eventRepository;
    }

    @Transactional
    public ShipmentSnapshot createShipment(Long orderId, String shipmentCode, String provider,
                                           String trackingNumber, String trackingUrl) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        if (shipmentRepository.findByShipmentCode(shipmentCode).isPresent()) {
            throw new IllegalArgumentException("Shipment code already exists");
        }

        ShipmentEntity shipment = new ShipmentEntity();
        shipment.setOrder(order);
        shipment.setShipmentCode(requireText(shipmentCode, "shipmentCode", 40));
        shipment.setProvider(normalizeProvider(provider));
        shipment.setTrackingNumber(cleanOptional(trackingNumber, 120));
        shipment.setTrackingUrl(cleanOptional(trackingUrl, 500));
        shipment.setStatus(ShipmentStatus.SHIPMENT_CREATED);
        ShipmentEntity saved = shipmentRepository.save(shipment);
        recordEvent(saved, saved.getStatus(), "Shipment created", OffsetDateTime.now());
        return toSnapshot(saved);
    }

    @Transactional
    public ShipmentSnapshot updateStatus(Long shipmentId, ShipmentStatus status, String description,
                                         OffsetDateTime occurredAt) {
        ShipmentEntity shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found: " + shipmentId));
        if (status == null) {
            throw new IllegalArgumentException("status is required");
        }
        shipment.setStatus(status);
        ShipmentEntity saved = shipmentRepository.save(shipment);
        recordEvent(saved, status, cleanOptional(description, 500), occurredAt == null ? OffsetDateTime.now() : occurredAt);
        return toSnapshot(saved);
    }

    @Transactional(readOnly = true)
    public List<ShipmentSnapshot> getOrderShipments(Long orderId) {
        return shipmentRepository.findByOrderIdOrderByCreatedAtAsc(orderId).stream()
                .map(this::toSnapshot)
                .toList();
    }

    private void recordEvent(ShipmentEntity shipment, ShipmentStatus status, String description,
                             OffsetDateTime occurredAt) {
        ShipmentStatusEventEntity event = new ShipmentStatusEventEntity();
        event.setShipment(shipment);
        event.setStatus(status);
        event.setDescription(description);
        event.setOccurredAt(occurredAt);
        eventRepository.save(event);
    }

    private ShipmentSnapshot toSnapshot(ShipmentEntity shipment) {
        List<ShipmentEventSnapshot> events = eventRepository
                .findByShipmentIdOrderByOccurredAtDesc(shipment.getId()).stream()
                .map(event -> new ShipmentEventSnapshot(event.getStatus(), event.getDescription(), event.getOccurredAt()))
                .toList();
        return new ShipmentSnapshot(
                shipment.getId(),
                shipment.getOrder().getId(),
                shipment.getShipmentCode(),
                shipment.getProvider(),
                shipment.getTrackingNumber(),
                shipment.getTrackingUrl(),
                shipment.getStatus(),
                events
        );
    }

    private String normalizeProvider(String provider) {
        return requireText(provider, "provider", 80).toUpperCase(Locale.ROOT);
    }

    private String requireText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        String cleaned = value.trim();
        if (cleaned.length() > maxLength) {
            throw new IllegalArgumentException(field + " exceeds " + maxLength + " characters");
        }
        return cleaned;
    }

    private String cleanOptional(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String cleaned = value.trim();
        if (cleaned.length() > maxLength) {
            throw new IllegalArgumentException("Value exceeds " + maxLength + " characters");
        }
        return cleaned;
    }
}
