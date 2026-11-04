package com.riceoil.platform.orders.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

    Optional<OrderEntity> findByIdempotencyKey(String idempotencyKey);

    List<OrderEntity> findByCustomerEmailOrderByIdDesc(String customerEmail);

    Optional<OrderEntity> findByIdAndCustomerEmail(Long id, String customerEmail);

    List<OrderEntity> findByCustomerIdOrderByIdDesc(Long customerId);

    Optional<OrderEntity> findByIdAndCustomerId(Long id, Long customerId);

    Optional<OrderEntity> findByOrderNumber(String orderNumber);

    List<OrderEntity> findTop200ByStatusOrderByIdDesc(OrderStatus status);

    Optional<OrderEntity> findByPaymentReference(String paymentReference);

    List<OrderEntity> findByStatusInAndCreatedAtBetween(List<OrderStatus> statuses,
                                                         java.time.OffsetDateTime from,
                                                         java.time.OffsetDateTime to);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OrderEntity o where o.id = :id")
    Optional<OrderEntity> findByIdForUpdate(@Param("id") Long id);
}
