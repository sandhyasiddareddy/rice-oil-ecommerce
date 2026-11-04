package com.riceoil.platform.shipping.application;

import com.riceoil.platform.notifications.EmailNotificationSender;
import com.riceoil.platform.orders.domain.OrderEntity;
import com.riceoil.platform.orders.domain.OrderRepository;
import com.riceoil.platform.orders.domain.OrderStatus;
import com.riceoil.platform.orders.domain.aftercare.OrderCancellationRequestRepository;
import com.riceoil.platform.orders.domain.aftercare.RequestStatus;
import com.riceoil.platform.shared.observability.BusinessLog;
import com.riceoil.platform.shared.web.ResourceNotFoundException;
import com.riceoil.platform.shipping.domain.ShipmentStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * One-step manual fulfilment for admins: identify the order by number, set the tracking details and
 * status. Creates the shipment on first use, then updates it, and emails the customer on progress.
 */
@Service
public class AdminShipmentUpdateService {

    private static final Set<ShipmentStatus> NOTIFY_ON = Set.of(
            ShipmentStatus.SHIPPED, ShipmentStatus.PICKED_UP, ShipmentStatus.IN_TRANSIT,
            ShipmentStatus.OUT_FOR_DELIVERY, ShipmentStatus.DELIVERED);

    private static final List<RequestStatus> OPEN_CANCELLATION = List.of(RequestStatus.REQUESTED, RequestStatus.PROCESSING);

    private final OrderRepository orderRepository;
    private final OrderCancellationRequestRepository cancellationRequestRepository;
    private final ShipmentTrackingService trackingService;
    private final EmailNotificationSender emailSender;
    private final String frontendBaseUrl;
    private final String deliveryEstimate;

    public AdminShipmentUpdateService(OrderRepository orderRepository,
                                      OrderCancellationRequestRepository cancellationRequestRepository,
                                      ShipmentTrackingService trackingService,
                                      EmailNotificationSender emailSender,
                                      @Value("${app.frontend-base-url}") String frontendBaseUrl,
                                      @Value("${app.orders.delivery-estimate:5-7 working days}") String deliveryEstimate) {
        this.orderRepository = orderRepository;
        this.cancellationRequestRepository = cancellationRequestRepository;
        this.trackingService = trackingService;
        this.emailSender = emailSender;
        this.frontendBaseUrl = frontendBaseUrl;
        this.deliveryEstimate = deliveryEstimate;
    }

    @Transactional
    public ShipmentSnapshot update(String orderNumber, String provider, String trackingNumber, String trackingUrl,
                                   ShipmentStatus status, String note) {
        if (orderNumber == null || orderNumber.isBlank()) {
            throw new IllegalArgumentException("orderNumber is required");
        }
        OrderEntity order = orderRepository.findByOrderNumber(orderNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderNumber.trim()));
        if (order.getStatus() != OrderStatus.PAID) {
            throw new IllegalArgumentException("Only paid orders can be shipped");
        }
        if (cancellationRequestRepository.existsByOrderIdAndStatusIn(order.getId(), OPEN_CANCELLATION)) {
            throw new IllegalStateException("The customer has asked to cancel this order; resolve the request first");
        }

        List<ShipmentSnapshot> existing = trackingService.getOrderShipments(order.getId());
        ShipmentSnapshot shipment;
        boolean firstCreate = existing.isEmpty();
        if (firstCreate) {
            String carrier = provider == null || provider.isBlank() ? "COURIER" : provider;
            shipment = trackingService.createShipment(order.getId(), "SH-" + order.getOrderNumber(), carrier,
                    trackingNumber, trackingUrl);
        } else {
            ShipmentSnapshot current = existing.get(existing.size() - 1);
            shipment = trackingService.updateTracking(current.id(), provider, trackingNumber, trackingUrl);
        }

        ShipmentStatus previous = firstCreate ? ShipmentStatus.SHIPMENT_CREATED : shipment.status();
        boolean statusChanged = status != null && (status != previous || (note != null && !note.isBlank()));
        if (statusChanged) {
            shipment = trackingService.updateStatus(shipment.id(), status, note, null);
        }
        BusinessLog.info("SHIPMENT_UPDATED_BY_ADMIN", "orderNumber", order.getOrderNumber(),
                "shipmentId", shipment.id(), "status", shipment.status(),
                "trackingNumber", shipment.trackingNumber());

        if (statusChanged && NOTIFY_ON.contains(status)) {
            notifyCustomer(order, shipment);
        }
        return shipment;
    }

    /** Paid orders with their fulfilment stage; READY_TO_PACK means no shipment record exists yet. */
    @Transactional(readOnly = true)
    public List<FulfilmentOrder> listPaidOrders() {
        return orderRepository.findTop200ByStatusOrderByIdDesc(OrderStatus.PAID).stream()
                .filter(order -> !cancellationRequestRepository.existsByOrderIdAndStatusIn(order.getId(), OPEN_CANCELLATION))
                .map(order -> {
            List<ShipmentSnapshot> shipments = trackingService.getOrderShipments(order.getId());
            ShipmentSnapshot latest = shipments.isEmpty() ? null : shipments.get(shipments.size() - 1);
            return new FulfilmentOrder(order.getId(), order.getOrderNumber(), order.getCustomerEmail(),
                    order.getTotalAmount(), order.getShippingAddressJson(), order.getPaymentLinkedAt(),
                    latest == null ? "READY_TO_PACK" : latest.status().name(),
                    latest == null ? null : latest.provider(),
                    latest == null ? null : latest.trackingNumber());
        }).toList();
    }

    public record FulfilmentOrder(Long orderId, String orderNumber, String customerEmail,
                                  java.math.BigDecimal totalAmount, String shippingAddressJson,
                                  java.time.OffsetDateTime paidAt, String fulfilmentStatus,
                                  String provider, String trackingNumber) {
    }

    private void notifyCustomer(OrderEntity order, ShipmentSnapshot shipment) {
        String recipient = order.getCustomerEmail();
        if (recipient == null || recipient.isBlank()) {
            return;
        }
        StringBuilder body = new StringBuilder("Update on your Farmora order ")
                .append(order.getOrderNumber()).append(":\n\nStatus: ")
                .append(shipment.status().name().replace('_', ' '));
        if (shipment.trackingNumber() != null) {
            body.append("\nTracking number: ").append(shipment.trackingNumber());
            if (shipment.provider() != null) {
                body.append(" (").append(shipment.provider()).append(')');
            }
        }
        if (shipment.status() != ShipmentStatus.DELIVERED) {
            body.append("\nEstimated delivery: ").append(deliveryEstimate).append(" after dispatch.");
        }
        body.append("\n\nTrack your order: ").append(order.getCustomerId() == null
                ? frontendBaseUrl + "/track-order?orderId=" + order.getId()
                : frontendBaseUrl + "/orders");
        try {
            emailSender.sendOrderUpdate(recipient,
                    "Farmora order " + order.getOrderNumber() + ": " + shipment.status().name().replace('_', ' ').toLowerCase(),
                    body.toString());
        } catch (RuntimeException ex) {
            BusinessLog.warn("SHIPMENT_EMAIL_FAILED", "orderNumber", order.getOrderNumber(), "error", ex.getMessage());
        }
    }
}
