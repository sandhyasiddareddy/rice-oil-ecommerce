package com.riceoil.platform.customers.application;

import com.riceoil.platform.customers.domain.CustomerAccountEntity;
import com.riceoil.platform.customers.domain.CustomerAccountRepository;
import com.riceoil.platform.customers.domain.CustomerAddressEntity;
import com.riceoil.platform.customers.domain.CustomerAddressRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerAccountServiceTest {

    @Mock
    private CustomerAccountRepository customerAccountRepository;

    @Mock
    private CustomerAddressRepository customerAddressRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private CustomerAccountService customerAccountService;

    @Test
    void addAddress_shouldUnsetExistingDefaultWhenNewDefaultAdded() throws Exception {
        CustomerAccountEntity customer = new CustomerAccountEntity();
        setField(customer, "id", 12L);
        customer.setFullName("Test User");
        customer.setEmail("test@example.com");

        CustomerAddressEntity existingDefault = new CustomerAddressEntity();
        setField(existingDefault, "id", 7L);
        existingDefault.setCustomer(customer);
        existingDefault.setLabel("Home");
        existingDefault.setFullName("Test User");
        existingDefault.setPhone("9999999999");
        existingDefault.setLine1("Old line");
        existingDefault.setCity("Bengaluru");
        existingDefault.setState("Karnataka");
        existingDefault.setPostalCode("560001");
        existingDefault.setCountry("IN");
        existingDefault.setDefaultAddress(true);
        existingDefault.setShippingAddress(true);
        existingDefault.setBillingAddress(false);

        when(customerAccountRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(customer));
        when(customerAddressRepository.findByCustomerIdOrderByCreatedAtAsc(12L)).thenReturn(List.of(existingDefault));
        when(customerAddressRepository.save(any(CustomerAddressEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerAddressEntity saved = customerAccountService.addAddress(
                "test@example.com",
                "Office",
                "Test User",
                "9876543210",
                "New line",
                "",
                "Bengaluru",
                "Karnataka",
                "560010",
                "IN",
                true,
                true,
                false
        );

        assertTrue(saved.isDefaultAddress());
        assertFalse(existingDefault.isDefaultAddress());
    }

    private static void setField(Object target, String fieldName, Long value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
