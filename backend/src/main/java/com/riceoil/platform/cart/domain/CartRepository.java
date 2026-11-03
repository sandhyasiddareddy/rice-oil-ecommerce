package com.riceoil.platform.cart.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepository extends JpaRepository<CartEntity, Long> {
    Optional<CartEntity> findFirstByCustomerIdAndStatusOrderByUpdatedAtDesc(Long customerId, CartStatus status);
}
