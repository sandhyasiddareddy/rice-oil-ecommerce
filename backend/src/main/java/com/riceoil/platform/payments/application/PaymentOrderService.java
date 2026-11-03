package com.riceoil.platform.payments.application;

import com.riceoil.platform.orders.application.GuestOrderAccessService;
import com.riceoil.platform.orders.domain.OrderEntity;
import com.riceoil.platform.orders.domain.OrderRepository;
import com.riceoil.platform.orders.domain.OrderStatus;
import com.riceoil.platform.shared.web.ResourceNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Locale;
import java.util.UUID;

@Service
public class PaymentOrderService {

    private static final String RAZORPAY = "RAZORPAY";

    private final OrderRepository orderRepository;
    private final GuestOrderAccessService guestOrderAccessService;

    public PaymentOrderService(OrderRepository orderRepository,
                               GuestOrderAccessService guestOrderAccessService) {
        this.orderRepository = orderRepository;
        this.guestOrderAccessService = guestOrderAccessService;
    }

    @Transactional
    public RazorpayCreateOrderResult createRazorpayOrder(Long orderId, Long customerId, String guestOrderToken) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        requireOwner(order, customerId, guestOrderToken);

        if (order.getPaymentReference() == null || order.getPaymentReference().isBlank()) {
            if (order.getStatus() != OrderStatus.CREATED) {
                throw new IllegalStateException("Payment cannot be started for an order in its current state");
            }
            order.setPaymentProvider(RAZORPAY);
            order.setPaymentReference(generatePaymentReference());
            order.setPaymentLinkedAt(java.time.OffsetDateTime.now());
            order.setStatus(OrderStatus.PAYMENT_PENDING);
            orderRepository.save(order);
        }

        return new RazorpayCreateOrderResult(
                order.getId(),
                order.getPaymentReference(),
                order.getCurrency(),
                order.getTotalAmount(),
                order.getStatus()
        );
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

    private String generatePaymentReference() {
        return "rpay_" + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 18)
                .toLowerCase(Locale.ROOT);
    }
}
