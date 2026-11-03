package com.riceoil.platform.orders.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

    Optional<OrderEntity> findByIdempotencyKey(String idempotencyKey);

    List<OrderEntity> findByCustomerEmailOrderByIdDesc(String customerEmail);

    Optional<OrderEntity> findByIdAndCustomerEmail(Long id, String customerEmail);

    List<OrderEntity> findByCustomerIdOrderByIdDesc(Long customerId);

    Optional<OrderEntity> findByIdAndCustomerId(Long id, Long customerId);

    Optional<OrderEntity> findByPaymentReference(String paymentReference);
}
