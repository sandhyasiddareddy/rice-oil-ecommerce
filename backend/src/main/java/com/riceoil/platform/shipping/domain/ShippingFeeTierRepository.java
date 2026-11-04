package com.riceoil.platform.shipping.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShippingFeeTierRepository extends JpaRepository<ShippingFeeTierEntity, Long> {

    List<ShippingFeeTierEntity> findAllByOrderByMinOrderAmountAsc();
}
