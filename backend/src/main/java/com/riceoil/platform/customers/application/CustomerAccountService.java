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
        List<CustomerAddressEntity> existingAddresses =
                customerAddressRepository.findByCustomerIdOrderByCreatedAtAsc(customer.getId());
        CustomerAddressEntity address = new CustomerAddressEntity();
        address.setCustomer(customer);
        boolean makeDefault = defaultAddress || existingAddresses.isEmpty();
        applyAddress(address, customer, label, fullName, phone, line1, line2, city, state, postalCode, country,
                makeDefault, shippingAddress || existingAddresses.isEmpty(), billingAddress);
        if (makeDefault) {
            clearDefaultAddress(existingAddresses);
        }
        return customerAddressRepository.save(address);
    }

    @Transactional
    public CustomerAddressEntity updateAddress(String email, Long addressId, String label, String fullName, String phone,
                                               String line1, String line2, String city, String state,
                                               String postalCode, String country, boolean defaultAddress,
                                               boolean shippingAddress, boolean billingAddress) {
        CustomerAccountEntity customer = getByEmail(email);
        CustomerAddressEntity address = getOwnedAddress(customer, addressId);
        List<CustomerAddressEntity> existingAddresses =
                customerAddressRepository.findByCustomerIdOrderByCreatedAtAsc(customer.getId());
        boolean wasDefault = address.isDefaultAddress();
        boolean noExistingDefault = existingAddresses.stream()
                .noneMatch(existing -> !existing.getId().equals(addressId) && existing.isDefaultAddress());
        boolean makeDefault = defaultAddress || noExistingDefault;
        applyAddress(address, customer, label, fullName, phone, line1, line2, city, state, postalCode, country,
                makeDefault, shippingAddress, billingAddress);
        if (makeDefault) {
            clearDefaultAddress(existingAddresses);
            address.setDefaultAddress(true);
        } else if (wasDefault) {
            address.setDefaultAddress(false);
            existingAddresses.stream()
                    .filter(existing -> !existing.getId().equals(addressId))
                    .findFirst()
                    .ifPresent(nextDefault -> {
                        nextDefault.setDefaultAddress(true);
                        customerAddressRepository.save(nextDefault);
                    });
        }
        return customerAddressRepository.save(address);
    }

    @Transactional
    public void deleteAddress(String email, Long addressId) {
        CustomerAccountEntity customer = getByEmail(email);
        CustomerAddressEntity address = getOwnedAddress(customer, addressId);
        boolean wasDefault = address.isDefaultAddress();
        customerAddressRepository.delete(address);
        if (wasDefault) {
            customerAddressRepository.findByCustomerIdOrderByCreatedAtAsc(customer.getId()).stream()
                    .findFirst()
                    .ifPresent(nextDefault -> {
                        nextDefault.setDefaultAddress(true);
                        customerAddressRepository.save(nextDefault);
                    });
        }
    }

    private CustomerAddressEntity getOwnedAddress(CustomerAccountEntity customer, Long addressId) {
        CustomerAddressEntity address = customerAddressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found: " + addressId));
        if (!address.getCustomer().getId().equals(customer.getId())) {
            throw new ResourceNotFoundException("Address not found: " + addressId);
        }
        return address;
    }

    private void clearDefaultAddress(List<CustomerAddressEntity> addresses) {
        addresses.stream()
                .filter(CustomerAddressEntity::isDefaultAddress)
                .forEach(existing -> {
                    existing.setDefaultAddress(false);
                    customerAddressRepository.save(existing);
                });
    }

    private void applyAddress(CustomerAddressEntity address, CustomerAccountEntity customer, String label,
                              String fullName, String phone, String line1, String line2, String city, String state,
                              String postalCode, String country, boolean defaultAddress, boolean shippingAddress,
                              boolean billingAddress) {
        address.setLabel(label == null || label.isBlank() ? "Home" : label.trim());
        address.setFullName(fullName == null || fullName.isBlank() ? customer.getFullName() : fullName.trim());
        address.setPhone(phone == null ? "" : phone.trim());
        address.setLine1(line1 == null ? "" : line1.trim());
        address.setLine2(line2 == null || line2.isBlank() ? null : line2.trim());
        address.setCity(city == null ? "" : city.trim());
        address.setState(state == null ? "" : state.trim());
        address.setPostalCode(postalCode == null ? "" : postalCode.trim());
        address.setCountry(country == null || country.isBlank() ? "IN" : country.trim().toUpperCase(Locale.ROOT));
        address.setDefaultAddress(defaultAddress);
        address.setShippingAddress(shippingAddress);
        address.setBillingAddress(billingAddress);
        if (address.getPhone().isBlank() || address.getLine1().isBlank() || address.getCity().isBlank()
                || address.getState().isBlank() || address.getPostalCode().isBlank()) {
            throw new IllegalArgumentException("Address fields are incomplete");
        }
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
