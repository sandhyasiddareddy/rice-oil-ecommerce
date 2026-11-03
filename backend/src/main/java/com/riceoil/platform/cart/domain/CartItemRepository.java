package com.riceoil.platform.cart.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItemEntity, Long> {

    List<CartItemEntity> findByCartIdOrderByIdAsc(Long cartId);

    Optional<CartItemEntity> findByIdAndCartId(Long id, Long cartId);
}
