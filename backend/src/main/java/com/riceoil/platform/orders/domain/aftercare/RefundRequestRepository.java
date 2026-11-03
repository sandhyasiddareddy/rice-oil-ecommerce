package com.riceoil.platform.orders.domain.aftercare;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RefundRequestRepository extends JpaRepository<RefundRequestEntity, Long> {

    List<RefundRequestEntity> findByOrderIdOrderByIdDesc(Long orderId);
}
