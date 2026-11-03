package com.riceoil.platform.orders.domain.aftercare;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderCancellationRequestRepository extends JpaRepository<OrderCancellationRequestEntity, Long> {

    List<OrderCancellationRequestEntity> findByOrderIdOrderByIdDesc(Long orderId);
}
