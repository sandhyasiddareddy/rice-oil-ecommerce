package com.riceoil.platform.orders.application.aftercare;

import com.riceoil.platform.orders.domain.OrderEntity;
import com.riceoil.platform.orders.domain.OrderRepository;
import com.riceoil.platform.orders.domain.OrderStatus;
import com.riceoil.platform.orders.domain.aftercare.OrderCancellationRequestEntity;
import com.riceoil.platform.orders.domain.aftercare.OrderCancellationRequestRepository;
import com.riceoil.platform.orders.domain.aftercare.RefundRequestEntity;
import com.riceoil.platform.orders.domain.aftercare.RefundRequestRepository;
import com.riceoil.platform.orders.domain.aftercare.RequestStatus;
import com.riceoil.platform.shared.web.ResourceNotFoundException;
import com.riceoil.platform.shipping.domain.ShipmentRepository;
import com.riceoil.platform.shipping.domain.ShipmentStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

@Service
public class AftercareRequestStateService {

    private static final List<RequestStatus> RESERVED_REFUND_STATUSES =
            List.of(RequestStatus.REQUESTED, RequestStatus.PROCESSING,
                    RequestStatus.PENDING, RequestStatus.PROCESSED);

    private final OrderRepository orderRepository;
    private final RefundRequestRepository refundRequestRepository;
    private final OrderCancellationRequestRepository cancellationRequestRepository;
    private final ShipmentRepository shipmentRepository;

    public AftercareRequestStateService(OrderRepository orderRepository,
                                        RefundRequestRepository refundRequestRepository,
                                        OrderCancellationRequestRepository cancellationRequestRepository,
                                        ShipmentRepository shipmentRepository) {
        this.orderRepository = orderRepository;
        this.refundRequestRepository = refundRequestRepository;
        this.cancellationRequestRepository = cancellationRequestRepository;
        this.shipmentRepository = shipmentRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminAftercareRequest> pendingRefundRequests() {
        return refundRequestRepository.findByStatusInOrderByRequestedAtAsc(
                        List.of(RequestStatus.REQUESTED, RequestStatus.PROCESSING)).stream()
                .map(this::refundSnapshot)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminAftercareRequest> pendingCancellationRequests() {
        return cancellationRequestRepository.findByStatusInOrderByRequestedAtAsc(
                        List.of(RequestStatus.REQUESTED, RequestStatus.PROCESSING)).stream()
                .map(this::cancellationSnapshot)
                .toList();
    }

    @Transactional(readOnly = true)
    public java.util.Optional<Long> orderIdOfRefund(Long refundRequestId) {
        return refundRequestRepository.findById(refundRequestId).map(r -> r.getOrder().getId());
    }

    @Transactional(readOnly = true)
    public java.util.Optional<Long> orderIdOfCancellation(Long cancellationRequestId) {
        return cancellationRequestRepository.findById(cancellationRequestId).map(c -> c.getOrder().getId());
    }

    @Transactional
    public RefundExecution beginRefundApproval(Long refundRequestId) {
        RefundRequestEntity refund = refundRequestRepository.findByIdForUpdate(refundRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Refund request not found: " + refundRequestId));
        OrderEntity order = orderRepository.findByIdForUpdate(refund.getOrder().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + refund.getOrder().getId()));
        if (refund.getStatus() != RequestStatus.REQUESTED) {
            throw new IllegalStateException("Refund request is no longer awaiting review");
        }
        if (order.getPaymentId() == null || !"RAZORPAY".equals(order.getPaymentProvider())) {
            throw new IllegalStateException("Order has no captured Razorpay payment to refund");
        }
        refund.setStatus(RequestStatus.PROCESSING);
        refundRequestRepository.saveAndFlush(refund);
        return new RefundExecution(refund.getId(), order.getId(), order.getPaymentId(), refund.getAmount());
    }

    @Transactional
    public void completeRefund(Long refundRequestId, String providerRefundId, String providerStatus) {
        RefundRequestEntity refund = refundRequestRepository.findByIdForUpdate(refundRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Refund request not found: " + refundRequestId));
        if (refund.getStatus() != RequestStatus.PROCESSING) {
            throw new IllegalStateException("Refund request is not processing");
        }
        refund.setProviderRefundId(providerRefundId);
        refund.setProviderStatus(providerStatus);
        refund.setStatus(refundStatus(providerStatus));
        refundRequestRepository.save(refund);
    }

    @Transactional
    public void rejectRefund(Long refundRequestId) {
        RefundRequestEntity refund = refundRequestRepository.findByIdForUpdate(refundRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Refund request not found: " + refundRequestId));
        if (refund.getStatus() != RequestStatus.REQUESTED) {
            throw new IllegalStateException("Refund request is no longer awaiting review");
        }
        refund.setStatus(RequestStatus.REJECTED);
        refundRequestRepository.save(refund);
    }

    @Transactional
    public CancellationExecution beginCancellationApproval(Long cancellationRequestId) {
        OrderCancellationRequestEntity cancellation = cancellationRequestRepository.findByIdForUpdate(cancellationRequestId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cancellation request not found: " + cancellationRequestId));
        OrderEntity order = orderRepository.findByIdForUpdate(cancellation.getOrder().getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found: " + cancellation.getOrder().getId()));
        if (cancellation.getStatus() != RequestStatus.REQUESTED) {
            throw new IllegalStateException("Cancellation request is no longer awaiting review");
        }
        if (order.getStatus() == OrderStatus.CANCELLED || isDelivered(order.getId())) {
            throw new IllegalStateException("This order can no longer be cancelled");
        }
        if (order.getStatus() == OrderStatus.CREATED) {
            order.setStatus(OrderStatus.CANCELLED);
            cancellation.setStatus(RequestStatus.APPROVED);
            orderRepository.save(order);
            cancellationRequestRepository.save(cancellation);
            return new CancellationExecution(cancellation.getId(), order.getId(), null, true);
        }
        if (order.getStatus() != OrderStatus.PAID
                || order.getPaymentId() == null
                || !"RAZORPAY".equals(order.getPaymentProvider())) {
            throw new IllegalStateException("Order payment must be settled before cancellation approval");
        }
        if (refundRequestRepository.existsByOrderIdAndStatusIn(order.getId(),
                List.of(RequestStatus.REQUESTED, RequestStatus.PROCESSING))) {
            throw new IllegalStateException("Resolve the existing refund request before approving cancellation");
        }

        BigDecimal alreadyReserved = refundRequestRepository.findByOrderIdOrderByIdDesc(order.getId()).stream()
                .filter(refund -> RESERVED_REFUND_STATUSES.contains(refund.getStatus()))
                .map(RefundRequestEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal remaining = order.getTotalAmount().subtract(alreadyReserved);
        RefundExecution refundExecution = null;
        if (remaining.signum() > 0) {
            RefundRequestEntity refund = new RefundRequestEntity();
            refund.setOrder(order);
            refund.setAmount(remaining);
            refund.setReason("Refund for approved order cancellation");
            refund.setStatus(RequestStatus.PROCESSING);
            refund = refundRequestRepository.saveAndFlush(refund);
            cancellation.setRefundRequest(refund);
            refundExecution = new RefundExecution(refund.getId(), order.getId(),
                    order.getPaymentId(), remaining);
        }
        cancellation.setStatus(RequestStatus.PROCESSING);
        cancellationRequestRepository.saveAndFlush(cancellation);
        return new CancellationExecution(cancellation.getId(), order.getId(), refundExecution, false);
    }

    @Transactional
    public void completeCancellation(Long cancellationRequestId) {
        OrderCancellationRequestEntity cancellation = cancellationRequestRepository.findByIdForUpdate(cancellationRequestId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cancellation request not found: " + cancellationRequestId));
        if (cancellation.getStatus() != RequestStatus.PROCESSING) {
            throw new IllegalStateException("Cancellation request is not processing");
        }
        OrderEntity order = orderRepository.findByIdForUpdate(cancellation.getOrder().getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found: " + cancellation.getOrder().getId()));
        if (isDelivered(order.getId())) {
            throw new IllegalStateException("This order has been shipped and can no longer be cancelled");
        }
        order.setStatus(OrderStatus.CANCELLED);
        cancellation.setStatus(RequestStatus.APPROVED);
        orderRepository.save(order);
        cancellationRequestRepository.save(cancellation);
    }

    @Transactional
    public void rejectCancellation(Long cancellationRequestId) {
        OrderCancellationRequestEntity cancellation = cancellationRequestRepository.findByIdForUpdate(cancellationRequestId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cancellation request not found: " + cancellationRequestId));
        if (cancellation.getStatus() != RequestStatus.REQUESTED) {
            throw new IllegalStateException("Cancellation request is no longer awaiting review");
        }
        cancellation.setStatus(RequestStatus.REJECTED);
        cancellationRequestRepository.save(cancellation);
    }

    private RequestStatus refundStatus(String providerStatus) {
        return switch (providerStatus.toLowerCase(Locale.ROOT)) {
            case "processed" -> RequestStatus.PROCESSED;
            case "failed" -> RequestStatus.FAILED;
            default -> RequestStatus.PENDING;
        };
    }

    private AdminAftercareRequest refundSnapshot(RefundRequestEntity refund) {
        OrderEntity order = refund.getOrder();
        return new AdminAftercareRequest(refund.getId(), order.getId(), order.getOrderNumber(),
                order.getCustomerEmail(), refund.getAmount(), refund.getStatus().name(), refund.getReason(),
                refund.getProviderRefundId(), refund.getProviderStatus());
    }

    private AdminAftercareRequest cancellationSnapshot(OrderCancellationRequestEntity cancellation) {
        OrderEntity order = cancellation.getOrder();
        return new AdminAftercareRequest(cancellation.getId(), order.getId(), order.getOrderNumber(),
                order.getCustomerEmail(), null, cancellation.getStatus().name(), cancellation.getReason(),
                null, null);
    }

    private boolean isDelivered(Long orderId) {
        return shipmentRepository.findByOrderIdOrderByCreatedAtAsc(orderId).stream()
                .anyMatch(shipment -> shipment.getStatus().isDispatched());
    }
}
