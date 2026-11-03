package com.riceoil.platform.customers.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerAddressRepository extends JpaRepository<CustomerAddressEntity, Long> {
    List<CustomerAddressEntity> findByCustomerOrderByCreatedAtAsc(CustomerAccountEntity customer);
    List<CustomerAddressEntity> findByCustomerIdOrderByCreatedAtAsc(Long customerId);
    List<CustomerAddressEntity> findByCustomerIdAndDefaultAddressTrueOrderByCreatedAtAsc(Long customerId);
    List<CustomerAddressEntity> findByCustomerIdAndShippingAddressTrueOrderByCreatedAtAsc(Long customerId);
    List<CustomerAddressEntity> findByCustomerIdAndBillingAddressTrueOrderByCreatedAtAsc(Long customerId);
}
