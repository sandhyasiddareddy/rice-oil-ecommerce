package com.riceoil.platform.orders.domain.aftercare;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface RefundRequestRepository extends JpaRepository<RefundRequestEntity, Long> {

    List<RefundRequestEntity> findByOrderIdOrderByIdDesc(Long orderId);

    List<RefundRequestEntity> findByStatusOrderByRequestedAtAsc(RequestStatus status);

    List<RefundRequestEntity> findByStatusInOrderByRequestedAtAsc(List<RequestStatus> statuses);

    Optional<RefundRequestEntity> findByProviderRefundId(String providerRefundId);

    boolean existsByOrderIdAndStatusIn(Long orderId, List<RequestStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RefundRequestEntity r where r.id = :id")
    java.util.Optional<RefundRequestEntity> findByIdForUpdate(@Param("id") Long id);
}
