package com.riceoil.platform.orders.application;

import com.riceoil.platform.notifications.EmailNotificationSender;
import com.riceoil.platform.orders.domain.OrderEntity;
import com.riceoil.platform.orders.domain.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Customer emails for cancellation and refund outcomes. Failures are logged and never block the admin action.
 */
@Service
public class OrderAftercareMailer {

    private static final Logger log = LoggerFactory.getLogger(OrderAftercareMailer.class);

    private final OrderRepository orderRepository;
    private final EmailNotificationSender emailSender;

    public OrderAftercareMailer(OrderRepository orderRepository, EmailNotificationSender emailSender) {
        this.orderRepository = orderRepository;
        this.emailSender = emailSender;
    }

    public void orderCancelled(Long orderId, boolean refundInitiated) {
        send(orderId, "has been cancelled",
                "Your order has been cancelled."
                        + (refundInitiated
                        ? " A refund has been initiated to your original payment method and usually reaches you within 5-7 business days."
                        : " No payment was taken for it."));
    }

    public void cancellationRejected(Long orderId) {
        send(orderId, "cancellation request update",
                "We could not cancel your order, so it will continue as normal. Please contact support if you have questions.");
    }

    public void refundInitiated(Long orderId, BigDecimal amount, String currency) {
        send(orderId, "refund initiated",
                "A refund of " + currency + " " + amount + " has been initiated to your original payment method. "
                        + "It usually reaches you within 5-7 business days, depending on your bank.");
    }

    public void refundRejected(Long orderId) {
        send(orderId, "refund request update",
                "We reviewed your refund request and could not approve it. Please contact support if you have questions.");
    }

    @Transactional(readOnly = true)
    void send(Long orderId, String subjectSuffix, String message) {
        try {
            OrderEntity order = orderRepository.findById(orderId).orElse(null);
            if (order == null || order.getCustomerEmail() == null || order.getCustomerEmail().isBlank()) {
                return;
            }
            emailSender.sendOrderUpdate(order.getCustomerEmail(),
                    "Your Farmora order " + order.getOrderNumber() + " " + subjectSuffix,
                    "Order " + order.getOrderNumber() + "\n\n" + message + "\n\nThank you for shopping with Farmora.");
        } catch (RuntimeException ex) {
            log.warn("Could not send aftercare email for order {}: {}", orderId, ex.getMessage());
        }
    }
}
