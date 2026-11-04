package com.riceoil.platform.orders.application.aftercare;

import com.riceoil.platform.orders.domain.OrderEntity;
import com.riceoil.platform.orders.domain.OrderRepository;
import com.riceoil.platform.orders.domain.OrderStatus;
import com.riceoil.platform.orders.application.GuestOrderAccessService;
import com.riceoil.platform.orders.domain.aftercare.OrderCancellationRequestEntity;
import com.riceoil.platform.orders.domain.aftercare.OrderCancellationRequestRepository;
import com.riceoil.platform.orders.domain.aftercare.RefundRequestEntity;
import com.riceoil.platform.orders.domain.aftercare.RefundRequestRepository;
import com.riceoil.platform.orders.domain.aftercare.RequestStatus;
import com.riceoil.platform.shared.observability.BusinessLog;
import com.riceoil.platform.shared.web.ResourceNotFoundException;
import com.riceoil.platform.shipping.domain.ShipmentRepository;
import com.riceoil.platform.shipping.domain.ShipmentStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Service
public class AftercareService {

    private final OrderRepository orderRepository;
    private final OrderCancellationRequestRepository cancellationRequestRepository;
    private final RefundRequestRepository refundRequestRepository;
    private final GuestOrderAccessService guestOrderAccessService;
    private final ShipmentRepository shipmentRepository;

    public AftercareService(OrderRepository orderRepository,
                            OrderCancellationRequestRepository cancellationRequestRepository,
                            RefundRequestRepository refundRequestRepository,
                            GuestOrderAccessService guestOrderAccessService,
                            ShipmentRepository shipmentRepository) {
        this.orderRepository = orderRepository;
        this.cancellationRequestRepository = cancellationRequestRepository;
        this.refundRequestRepository = refundRequestRepository;
        this.guestOrderAccessService = guestOrderAccessService;
        this.shipmentRepository = shipmentRepository;
    }

    @Transactional
    public CancellationSnapshot requestCancellation(Long orderId, String reason,
                                                      Long customerId, String guestOrderToken) {
        OrderEntity order = getOrderForUpdate(orderId);
        requireOwner(order, customerId, guestOrderToken);
        if (order.getStatus() == OrderStatus.CANCELLED || isDelivered(orderId)) {
            throw new IllegalStateException("This order can no longer be cancelled");
        }
        if (cancellationRequestRepository.existsByOrderIdAndStatusIn(orderId,
                List.of(RequestStatus.REQUESTED, RequestStatus.PROCESSING))) {
            throw new IllegalStateException("A cancellation request is already being reviewed");
        }

        OrderCancellationRequestEntity entity = new OrderCancellationRequestEntity();
        entity.setOrder(order);
        entity.setReason(cleanReason(reason));
        entity.setStatus(RequestStatus.REQUESTED);

        OrderCancellationRequestEntity saved = cancellationRequestRepository.save(entity);
        BusinessLog.info("CANCELLATION_REQUESTED", "cancellationRequestId", saved.getId(),
                "orderNumber", order.getOrderNumber(), "orderId", orderId, "orderStatus", order.getStatus(),
                "razorpayPaymentId", order.getPaymentId(), "reason", saved.getReason());
        return new CancellationSnapshot(saved.getId(), orderId, saved.getStatus().name(), saved.getReason());
    }

    @Transactional
    public RefundSnapshot requestRefund(Long orderId, BigDecimal amount, String reason,
                                        Long customerId, String guestOrderToken) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Refund amount must be greater than zero");
        }
        try {
            amount = amount.setScale(2, java.math.RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("Refund amount must have no more than two decimal places");
        }

        OrderEntity order = getOrderForUpdate(orderId);
        requireOwner(order, customerId, guestOrderToken);
        if (order.getStatus() != OrderStatus.PAID
                || order.getPaymentId() == null
                || !"RAZORPAY".equals(order.getPaymentProvider())) {
            throw new IllegalStateException("Only captured Razorpay payments can be refunded");
        }
        BigDecimal alreadyRequested = refundRequestRepository.findByOrderIdOrderByIdDesc(orderId).stream()
                .filter(refund -> refund.getStatus() != RequestStatus.REJECTED
                        && refund.getStatus() != RequestStatus.FAILED)
                .map(RefundRequestEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (amount.add(alreadyRequested).compareTo(order.getTotalAmount()) > 0) {
            throw new IllegalArgumentException("Refund amount exceeds the remaining refundable balance");
        }

        RefundRequestEntity entity = new RefundRequestEntity();
        entity.setOrder(order);
        entity.setAmount(amount);
        entity.setReason(cleanReason(reason));
        entity.setStatus(RequestStatus.REQUESTED);

        RefundRequestEntity saved = refundRequestRepository.save(entity);
        BusinessLog.info("REFUND_REQUESTED", "refundRequestId", saved.getId(), "orderNumber", order.getOrderNumber(),
                "orderId", orderId, "amount", amount, "orderTotal", order.getTotalAmount(),
                "razorpayPaymentId", order.getPaymentId(), "reason", saved.getReason());
        return toRefundSnapshot(saved);
    }

    @Transactional(readOnly = true)
    public List<RefundSnapshot> refunds(Long orderId, Long customerId, String guestOrderToken) {
        OrderEntity order = getOrder(orderId);
        requireOwner(order, customerId, guestOrderToken);
        return refundRequestRepository.findByOrderIdOrderByIdDesc(orderId).stream()
                .map(this::toRefundSnapshot)
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean canRequestCancellation(Long orderId) {
        OrderEntity order = getOrder(orderId);
        return (order.getStatus() == OrderStatus.CREATED || order.getStatus() == OrderStatus.PAID)
                && !isDelivered(orderId)
                && !cancellationRequestRepository.existsByOrderIdAndStatusIn(orderId,
                List.of(RequestStatus.REQUESTED, RequestStatus.PROCESSING));
    }

    @Transactional(readOnly = true)
    public AftercareSummary summary(Long orderId) {
        String cancellationStatus = cancellationRequestRepository.findByOrderIdOrderByIdDesc(orderId).stream()
                .findFirst().map(request -> request.getStatus().name()).orElse(null);
        List<RefundRequestEntity> refunds = refundRequestRepository.findByOrderIdOrderByIdDesc(orderId);
        String refundStatus = refunds.stream().findFirst().map(refund -> refund.getStatus().name()).orElse(null);
        BigDecimal refundAmount = refunds.stream()
                .filter(refund -> refund.getStatus() != RequestStatus.REJECTED
                        && refund.getStatus() != RequestStatus.FAILED)
                .map(RefundRequestEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new AftercareSummary(canRequestCancellation(orderId), cancellationStatus, refundStatus,
                refunds.isEmpty() ? null : refundAmount);
    }

    private RefundSnapshot toRefundSnapshot(RefundRequestEntity entity) {
        return new RefundSnapshot(entity.getId(), entity.getOrder().getId(), entity.getAmount(),
                entity.getStatus().name(), entity.getReason(), entity.getProviderRefundId(), entity.getProviderStatus());
    }

    private OrderEntity getOrderForUpdate(Long orderId) {
        return orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
    }

    private OrderEntity getOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
    }

    private boolean isDelivered(Long orderId) {
        return shipmentRepository.findByOrderIdOrderByCreatedAtAsc(orderId).stream()
                .anyMatch(shipment -> shipment.getStatus().isDispatched());
    }

    private void requireOwner(OrderEntity order, Long customerId, String guestOrderToken) {
        if (order.getCustomerId() != null) {
            if (customerId == null || !Objects.equals(order.getCustomerId(), customerId)) {
                throw new AccessDeniedException("Order does not belong to the authenticated customer");
            }
            return;
        }
        if (customerId != null || guestOrderToken == null || guestOrderToken.isBlank()) {
            throw new AccessDeniedException("Guest order access token is required");
        }
        guestOrderAccessService.verifyAndGetOrder(order.getId(), guestOrderToken);
    }

    private String cleanReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return null;
        }
        String cleaned = reason.trim();
        if (cleaned.length() > 300) {
            throw new IllegalArgumentException("Reason exceeds 300 characters");
        }
        return cleaned;
    }
}
