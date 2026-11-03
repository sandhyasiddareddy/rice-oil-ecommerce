package com.riceoil.platform.orders.application.aftercare;

import com.riceoil.platform.orders.domain.OrderEntity;
import com.riceoil.platform.orders.domain.OrderRepository;
import com.riceoil.platform.orders.domain.aftercare.OrderCancellationRequestEntity;
import com.riceoil.platform.orders.domain.aftercare.OrderCancellationRequestRepository;
import com.riceoil.platform.orders.domain.aftercare.RefundRequestEntity;
import com.riceoil.platform.orders.domain.aftercare.RefundRequestRepository;
import com.riceoil.platform.orders.domain.aftercare.RequestStatus;
import com.riceoil.platform.shared.web.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class AftercareService {

    private final OrderRepository orderRepository;
    private final OrderCancellationRequestRepository cancellationRequestRepository;
    private final RefundRequestRepository refundRequestRepository;

    public AftercareService(OrderRepository orderRepository,
                            OrderCancellationRequestRepository cancellationRequestRepository,
                            RefundRequestRepository refundRequestRepository) {
        this.orderRepository = orderRepository;
        this.cancellationRequestRepository = cancellationRequestRepository;
        this.refundRequestRepository = refundRequestRepository;
    }

    @Transactional
    public CancellationSnapshot requestCancellation(Long orderId, String reason) {
        OrderEntity order = getOrder(orderId);

        OrderCancellationRequestEntity entity = new OrderCancellationRequestEntity();
        entity.setOrder(order);
        entity.setReason(reason);
        entity.setStatus(RequestStatus.REQUESTED);

        OrderCancellationRequestEntity saved = cancellationRequestRepository.save(entity);
        return new CancellationSnapshot(saved.getId(), orderId, saved.getStatus().name(), reason);
    }

    @Transactional
    public RefundSnapshot requestRefund(Long orderId, BigDecimal amount, String reason) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Refund amount must be greater than zero");
        }

        OrderEntity order = getOrder(orderId);
        if (amount.compareTo(order.getTotalAmount()) > 0) {
            throw new IllegalArgumentException("Refund amount cannot exceed order total");
        }

        RefundRequestEntity entity = new RefundRequestEntity();
        entity.setOrder(order);
        entity.setAmount(amount);
        entity.setReason(reason);
        entity.setStatus(RequestStatus.REQUESTED);

        RefundRequestEntity saved = refundRequestRepository.save(entity);
        return new RefundSnapshot(saved.getId(), orderId, amount, saved.getStatus().name(), reason);
    }

    @Transactional(readOnly = true)
    public List<RefundSnapshot> refunds(Long orderId) {
        getOrder(orderId);
        return refundRequestRepository.findByOrderIdOrderByIdDesc(orderId).stream()
                .map(entity -> new RefundSnapshot(
                        entity.getId(),
                        orderId,
                        entity.getAmount(),
                        entity.getStatus().name(),
                        entity.getReason()
                ))
                .toList();
    }

    private OrderEntity getOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
    }
}
