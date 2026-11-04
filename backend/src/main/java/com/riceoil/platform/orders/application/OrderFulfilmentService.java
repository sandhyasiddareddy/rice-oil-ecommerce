package com.riceoil.platform.orders.application;

import com.riceoil.platform.cart.domain.CartItemEntity;
import com.riceoil.platform.cart.domain.CartItemRepository;
import com.riceoil.platform.catalog.domain.ProductVariantRepository;
import com.riceoil.platform.inventory.application.InventoryReservationService;
import com.riceoil.platform.notifications.EmailNotificationSender;
import com.riceoil.platform.orders.domain.OrderEntity;
import com.riceoil.platform.orders.domain.OrderRepository;
import com.riceoil.platform.orders.domain.OrderStatus;
import com.riceoil.platform.shared.observability.BusinessLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Stock reservation, post-payment confirmation and expiry of unpaid orders.
 */
@Service
public class OrderFulfilmentService {

    private static final Logger log = LoggerFactory.getLogger(OrderFulfilmentService.class);

    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository productVariantRepository;
    private final InventoryReservationService reservationService;
    private final OrderRepository orderRepository;
    private final GuestOrderAccessService guestOrderAccessService;
    private final EmailNotificationSender emailSender;
    private final String frontendBaseUrl;
    private final long unpaidExpiryMinutes;
    private final long lookbackHours;
    private final String deliveryEstimate;
    public OrderFulfilmentService(CartItemRepository cartItemRepository,
                                  ProductVariantRepository productVariantRepository,
                                  InventoryReservationService reservationService,
                                  OrderRepository orderRepository,
                                  GuestOrderAccessService guestOrderAccessService,
                                  EmailNotificationSender emailSender,
                                  @Value("${app.frontend-base-url}") String frontendBaseUrl,
                                  @Value("${app.orders.unpaid-expiry-minutes:60}") long unpaidExpiryMinutes,
                                  @Value("${app.orders.expiry-lookback-hours:48}") long lookbackHours,
                                  @Value("${app.orders.delivery-estimate:5-7 working days}") String deliveryEstimate) {
        this.deliveryEstimate = deliveryEstimate;
        this.lookbackHours = lookbackHours;
        this.cartItemRepository = cartItemRepository;
        this.productVariantRepository = productVariantRepository;
        this.reservationService = reservationService;
        this.orderRepository = orderRepository;
        this.guestOrderAccessService = guestOrderAccessService;
        this.emailSender = emailSender;
        this.frontendBaseUrl = frontendBaseUrl;
        this.unpaidExpiryMinutes = unpaidExpiryMinutes;
    }

    /** Holds stock for the order; throws if any line is not available. Call inside the order transaction. */
    @Transactional
    public void reserveStock(OrderEntity order) {
        OffsetDateTime expiresAt = OffsetDateTime.now().plusHours(lookbackHours);
        for (CartItemEntity item : cartItemRepository.findByCartIdOrderByIdAsc(order.getCart().getId())) {
            productVariantRepository.findBySku(item.getSku()).ifPresent(variant -> {
                reservationService.reserve(variant.getId(), item.getQuantity(),
                        reservationPrefix(order) + variant.getId(), expiresAt, item.getProductName());
                BusinessLog.info("STOCK_RESERVED", "orderNumber", order.getOrderNumber(),
                        "sku", item.getSku(), "quantity", item.getQuantity());
            });
        }
    }

    /** Called once when an order moves to PAID. Never fails the payment flow. */
    @Transactional
    public void onPaid(OrderEntity order) {
        reservationService.consume(reservationPrefix(order));
        BusinessLog.info("ORDER_PAID", "orderNumber", order.getOrderNumber(), "orderId", order.getId(),
                "total", order.getTotalAmount(), "currency", order.getCurrency(),
                "razorpayOrderId", order.getPaymentReference(), "razorpayPaymentId", order.getPaymentId());
        String recipient = order.getCustomerEmail();
        if (recipient == null || recipient.isBlank()) {
            return;
        }
        String trackingUrl = order.getCustomerId() == null
                ? frontendBaseUrl + "/track-order?orderId=" + order.getId()
                + "&token=" + guestOrderAccessService.issueToken(order.getId()).accessToken()
                : frontendBaseUrl + "/orders";
        String body = buildConfirmation(order, trackingUrl);
        Runnable send = () -> {
            try {
                emailSender.sendOrderConfirmation(recipient, order.getOrderNumber(), body);
                BusinessLog.info("ORDER_CONFIRMATION_EMAIL_SENT", "orderNumber", order.getOrderNumber());
            } catch (RuntimeException ex) {
                BusinessLog.warn("ORDER_CONFIRMATION_EMAIL_FAILED", "orderNumber", order.getOrderNumber(),
                        "reason", ex.getMessage());
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send.run();
                }
            });
        } else {
            send.run();
        }
    }

    @Scheduled(fixedDelayString = "${app.orders.expiry-check-interval-ms:300000}")
    @Transactional
    public int expireUnpaidOrders() {
        OffsetDateTime cutoff = OffsetDateTime.now().minusMinutes(unpaidExpiryMinutes);
        List<OrderEntity> stale = orderRepository.findByStatusInAndCreatedAtBetween(
                List.of(OrderStatus.CREATED, OrderStatus.PAYMENT_PENDING),
                OffsetDateTime.now().minusHours(lookbackHours), cutoff);
        for (OrderEntity order : stale) {
            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);
            reservationService.release(reservationPrefix(order));
            BusinessLog.info("ORDER_EXPIRED", "orderNumber", order.getOrderNumber(), "orderId", order.getId(),
                    "razorpayOrderId", order.getPaymentReference(), "unpaidMinutes", unpaidExpiryMinutes);
        }
        return stale.size();
    }

    private String buildConfirmation(OrderEntity order, String trackingUrl) {
        StringBuilder text = new StringBuilder();
        text.append("Thank you for your order with Farmora!\n\n")
                .append("Order number: ").append(order.getOrderNumber()).append("\n\n");
        for (CartItemEntity item : cartItemRepository.findByCartIdOrderByIdAsc(order.getCart().getId())) {
            text.append("- ").append(item.getProductName()).append(" x ").append(item.getQuantity())
                    .append(": ").append(order.getCurrency()).append(' ').append(item.getLineTotal()).append('\n');
        }
        text.append("\nShipping: ").append(order.getCurrency()).append(' ').append(order.getShippingAmount())
                .append("\nTotal paid: ").append(order.getCurrency()).append(' ').append(order.getTotalAmount())
                .append("\n\nEstimated delivery: ").append(deliveryEstimate).append(" after dispatch.")
                .append("\n\nTrack your order here:\n").append(trackingUrl)
                .append("\n\nKeep this link private; it gives access to your order.");
        return text.toString();
    }

    private static String reservationPrefix(OrderEntity order) {
        return order.getOrderNumber() + ":";
    }
}
