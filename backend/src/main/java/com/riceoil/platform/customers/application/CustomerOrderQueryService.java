package com.riceoil.platform.customers.application;

import com.riceoil.platform.orders.domain.OrderEntity;
import com.riceoil.platform.orders.domain.OrderRepository;
import com.riceoil.platform.orders.domain.OrderStatus;
import com.riceoil.platform.customers.domain.CustomerAccountEntity;
import com.riceoil.platform.customers.domain.CustomerAccountRepository;
import com.riceoil.platform.shared.web.ResourceNotFoundException;
import com.riceoil.platform.orders.application.aftercare.AftercareService;
import com.riceoil.platform.orders.application.aftercare.AftercareSummary;
import com.riceoil.platform.orders.application.OrderItemsQueryService;
import com.riceoil.platform.orders.application.OrderLinesSnapshot;
import com.riceoil.platform.shipping.application.ShipmentTrackingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class CustomerOrderQueryService {

    private final OrderRepository orderRepository;
    private final CustomerAccountRepository customerAccountRepository;
    private final AftercareService aftercareService;
    private final OrderItemsQueryService orderItemsQueryService;
    private final ShipmentTrackingService shipmentTrackingService;

    public CustomerOrderQueryService(OrderRepository orderRepository,
                                     CustomerAccountRepository customerAccountRepository,
                                     AftercareService aftercareService,
                                     OrderItemsQueryService orderItemsQueryService,
                                     ShipmentTrackingService shipmentTrackingService) {
        this.orderRepository = orderRepository;
        this.customerAccountRepository = customerAccountRepository;
        this.aftercareService = aftercareService;
        this.orderItemsQueryService = orderItemsQueryService;
        this.shipmentTrackingService = shipmentTrackingService;
    }

    @Transactional(readOnly = true)
    public List<CustomerOrderSnapshot> listOrdersForCustomer(String customerEmail) {
        String normalizedEmail = normalizeEmail(customerEmail);

        CustomerAccountEntity customer = findCustomer(normalizedEmail);
        return orderRepository.findByCustomerIdOrderByIdDesc(customer.getId()).stream()
                .map(this::toSnapshot)
                .toList();
    }

    @Transactional(readOnly = true)
    public CustomerOrderSnapshot getOrderForCustomer(String customerEmail, Long orderId) {
        String normalizedEmail = normalizeEmail(customerEmail);

        CustomerAccountEntity customer = findCustomer(normalizedEmail);
        OrderEntity entity = orderRepository.findByIdAndCustomerId(orderId, customer.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        return toSnapshot(entity);
    }

    private String normalizeEmail(String customerEmail) {
        if (customerEmail == null || customerEmail.isBlank()) {
            throw new IllegalArgumentException("Authenticated customer identity is required");
        }
        return customerEmail.trim().toLowerCase(Locale.ROOT);
    }

    private CustomerAccountEntity findCustomer(String email) {
        return customerAccountRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Customer account not found"));
    }

    private CustomerOrderSnapshot toSnapshot(OrderEntity entity) {
        AftercareSummary aftercare = aftercareService.summary(entity.getId());
        OrderLinesSnapshot lines = orderItemsQueryService.linesFor(entity.getId());
        return new CustomerOrderSnapshot(
                entity.getId(),
                entity.getOrderNumber(),
                entity.getStatus().name(),
                entity.getCurrency(),
                entity.getTotalAmount(),
                lines.shippingAmount(),
                lines.items(),
                shipmentTrackingService.getOrderShipments(entity.getId()),
                entity.getCustomerEmail(),
                aftercare.canRequestCancellation(),
                aftercare.cancellationStatus(),
                aftercare.refundStatus(),
                aftercare.refundAmount()
        );
    }
}
