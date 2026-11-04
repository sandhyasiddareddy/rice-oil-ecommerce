package com.riceoil.platform.payments.application;

import com.riceoil.platform.orders.application.GuestOrderAccessService;
import com.riceoil.platform.orders.domain.OrderEntity;
import com.riceoil.platform.orders.domain.OrderRepository;
import com.riceoil.platform.orders.domain.OrderStatus;
import com.riceoil.platform.shared.observability.BusinessLog;
import com.riceoil.platform.shared.web.ResourceNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.beans.factory.annotation.Value;

@Service
public class PaymentOrderService {

    private static final String RAZORPAY = "RAZORPAY";

    private final OrderRepository orderRepository;
    private final GuestOrderAccessService guestOrderAccessService;
    private final RazorpayGateway razorpayGateway;
    private final String razorpayKeyId;

    public PaymentOrderService(OrderRepository orderRepository,
                               GuestOrderAccessService guestOrderAccessService,
                               RazorpayGateway razorpayGateway,
                               @Value("${app.payments.razorpay.key-id:}") String razorpayKeyId) {
        this.orderRepository = orderRepository;
        this.guestOrderAccessService = guestOrderAccessService;
        this.razorpayGateway = razorpayGateway;
        this.razorpayKeyId = razorpayKeyId == null || razorpayKeyId.isBlank()
                ? razorpayGateway.keyId()
                : razorpayKeyId.trim();
    }

    @Transactional
    public RazorpayCreateOrderResult createRazorpayOrder(Long orderId, Long customerId, String guestOrderToken) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        requireOwner(order, customerId, guestOrderToken);

        if (order.getPaymentReference() != null
                && !order.getPaymentReference().isBlank()
                && !RAZORPAY.equals(order.getPaymentProvider())) {
            throw new IllegalStateException("Order is linked to a different payment provider");
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("This order has expired or been cancelled and can no longer be paid");
        }

        if (order.getPaymentReference() == null || order.getPaymentReference().isBlank()) {
            if (order.getStatus() != OrderStatus.CREATED) {
                throw new IllegalStateException("Payment cannot be started for an order in its current state");
            }
            long amountInSubunits = order.getTotalAmount()
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(0, RoundingMode.UNNECESSARY)
                    .longValueExact();
            String providerOrderId = razorpayGateway.createOrder(
                    amountInSubunits,
                    order.getCurrency(),
                    order.getOrderNumber()
            );
            order.setPaymentProvider(RAZORPAY);
            order.setPaymentReference(providerOrderId);
            order.setPaymentLinkedAt(java.time.OffsetDateTime.now());
            order.setStatus(OrderStatus.PAYMENT_PENDING);
            orderRepository.save(order);
            BusinessLog.info("RAZORPAY_ORDER_CREATED", "orderNumber", order.getOrderNumber(), "orderId", order.getId(),
                    "razorpayOrderId", providerOrderId, "amountSubunits", amountInSubunits,
                    "currency", order.getCurrency());
        } else {
            BusinessLog.info("RAZORPAY_ORDER_REUSED", "orderNumber", order.getOrderNumber(), "orderId", order.getId(),
                    "razorpayOrderId", order.getPaymentReference(), "status", order.getStatus());
        }

        return new RazorpayCreateOrderResult(
                order.getId(),
                order.getPaymentReference(),
                order.getCurrency(),
                toSubunits(order.getTotalAmount()),
                order.getStatus(),
                razorpayKeyId
        );
    }

    private long toSubunits(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.UNNECESSARY)
                .longValueExact();
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

}
