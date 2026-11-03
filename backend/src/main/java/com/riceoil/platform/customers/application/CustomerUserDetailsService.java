package com.riceoil.platform.customers.application;

import com.riceoil.platform.customers.domain.CustomerAccountEntity;
import com.riceoil.platform.customers.domain.CustomerAccountRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Primary
public class CustomerUserDetailsService implements UserDetailsService {

    private final CustomerAccountRepository customerAccountRepository;

    public CustomerUserDetailsService(CustomerAccountRepository customerAccountRepository) {
        this.customerAccountRepository = customerAccountRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        CustomerAccountEntity customer = customerAccountRepository.findByEmailIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("Customer not found: " + username));

        return User.withUsername(customer.getEmail())
                .password(customer.getPasswordHash())
                .roles(extractRole(customer.getRole()))
                .build();
    }

    private String extractRole(String role) {
        if (role == null) {
            return "CUSTOMER";
        }
        return role.replace("ROLE_", "").trim();
    }
}
