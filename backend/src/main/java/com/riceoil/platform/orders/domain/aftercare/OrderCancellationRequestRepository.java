package com.riceoil.platform.orders.domain.aftercare;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;

public interface OrderCancellationRequestRepository extends JpaRepository<OrderCancellationRequestEntity, Long> {

    List<OrderCancellationRequestEntity> findByOrderIdOrderByIdDesc(Long orderId);

    List<OrderCancellationRequestEntity> findByStatusOrderByRequestedAtAsc(RequestStatus status);

    List<OrderCancellationRequestEntity> findByStatusInOrderByRequestedAtAsc(List<RequestStatus> statuses);

    boolean existsByOrderIdAndStatusIn(Long orderId, List<RequestStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from OrderCancellationRequestEntity c where c.id = :id")
    java.util.Optional<OrderCancellationRequestEntity> findByIdForUpdate(@Param("id") Long id);
}
