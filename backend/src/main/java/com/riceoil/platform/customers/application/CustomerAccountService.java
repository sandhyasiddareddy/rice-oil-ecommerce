package com.riceoil.platform.customers.application;

import com.riceoil.platform.customers.domain.CustomerAccountEntity;
import com.riceoil.platform.customers.domain.CustomerAccountRepository;
import com.riceoil.platform.customers.domain.CustomerAddressEntity;
import com.riceoil.platform.customers.domain.CustomerAddressRepository;
import com.riceoil.platform.shared.web.ResourceNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class CustomerAccountService {

    private final CustomerAccountRepository customerAccountRepository;
    private final CustomerAddressRepository customerAddressRepository;
    private final PasswordEncoder passwordEncoder;

    public CustomerAccountService(CustomerAccountRepository customerAccountRepository,
                                 CustomerAddressRepository customerAddressRepository,
                                 PasswordEncoder passwordEncoder) {
        this.customerAccountRepository = customerAccountRepository;
        this.customerAddressRepository = customerAddressRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public CustomerAccountEntity register(String fullName, String email, String password) {
        String normalizedName = normalizeName(fullName);
        String normalizedEmail = normalizeEmail(email);
        if (normalizedName.isBlank()) {
            throw new IllegalArgumentException("fullName is required");
        }
        if (normalizedEmail.isBlank() || password == null || password.isBlank()) {
            throw new IllegalArgumentException("email and password are required");
        }
        if (customerAccountRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new IllegalArgumentException("An account already exists for this email");
        }

        CustomerAccountEntity customer = new CustomerAccountEntity();
        customer.setFullName(normalizedName);
        customer.setEmail(normalizedEmail);
        customer.setPasswordHash(passwordEncoder.encode(password));
        customer.setRole("ROLE_CUSTOMER");
        customer.setEnabled(true);
        return customerAccountRepository.save(customer);
    }

    @Transactional(readOnly = true)
    public CustomerAccountEntity getByEmail(String email) {
        return customerAccountRepository.findByEmailIgnoreCase(normalizeEmail(email))
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + email));
    }

    @Transactional(readOnly = true)
    public List<CustomerAddressEntity> listAddresses(String email) {
        CustomerAccountEntity customer = getByEmail(email);
        return customerAddressRepository.findByCustomerIdOrderByCreatedAtAsc(customer.getId());
    }

    @Transactional
    public CustomerAddressEntity addAddress(String email, String label, String fullName, String phone,
                                           String line1, String line2, String city, String state,
                                           String postalCode, String country, boolean defaultAddress,
                                           boolean shippingAddress, boolean billingAddress) {
        CustomerAccountEntity customer = getByEmail(email);
        List<CustomerAddressEntity> existingAddresses = customerAddressRepository.findByCustomerIdOrderByCreatedAtAsc(customer.getId());

        CustomerAddressEntity address = new CustomerAddressEntity();
        address.setCustomer(customer);
        address.setLabel(label == null || label.isBlank() ? "Home" : label.trim());
        address.setFullName(fullName == null || fullName.isBlank() ? customer.getFullName() : fullName.trim());
        address.setPhone(phone == null ? "" : phone.trim());
        address.setLine1(line1 == null ? "" : line1.trim());
        address.setLine2(line2 == null ? null : line2.trim());
        address.setCity(city == null ? "" : city.trim());
        address.setState(state == null ? "" : state.trim());
        address.setPostalCode(postalCode == null ? "" : postalCode.trim());
        address.setCountry(country == null || country.isBlank() ? "IN" : country.trim().toUpperCase(Locale.ROOT));
        address.setDefaultAddress(defaultAddress);
        address.setShippingAddress(shippingAddress);
        address.setBillingAddress(billingAddress);

        if (address.getLine1().isBlank() || address.getCity().isBlank() || address.getState().isBlank() || address.getPostalCode().isBlank()) {
            throw new IllegalArgumentException("Address fields are incomplete");
        }

        if (defaultAddress) {
            existingAddresses.forEach(existing -> {
                if (existing.isDefaultAddress()) {
                    existing.setDefaultAddress(false);
                    customerAddressRepository.save(existing);
                }
            });
        }

        if (!shippingAddress && !billingAddress && existingAddresses.isEmpty()) {
            address.setShippingAddress(true);
        }

        return customerAddressRepository.save(address);
    }

    @Transactional(readOnly = true)
    public CustomerAccountEntity authenticate(String email, String password) {
        CustomerAccountEntity customer = customerAccountRepository.findByEmailIgnoreCase(normalizeEmail(email))
                .orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(password, customer.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        return customer;
    }

    private String normalizeName(String name) {
        return name == null ? "" : name.trim();
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
