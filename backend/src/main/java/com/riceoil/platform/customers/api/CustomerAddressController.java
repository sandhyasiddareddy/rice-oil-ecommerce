package com.riceoil.platform.customers.api;

import com.riceoil.platform.customers.application.CustomerAccountService;
import com.riceoil.platform.customers.domain.CustomerAddressEntity;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerAddressController {

    private final CustomerAccountService customerAccountService;

    public CustomerAddressController(CustomerAccountService customerAccountService) {
        this.customerAccountService = customerAccountService;
    }

    @GetMapping("/addresses")
    public List<CustomerAddressResponse> listAddresses(@AuthenticationPrincipal Jwt jwt) {
        return customerAccountService.listAddresses(jwt.getSubject()).stream()
                .map(CustomerAddressController::toResponse)
                .toList();
    }

    @PostMapping("/addresses")
    public CustomerAddressResponse addAddress(@AuthenticationPrincipal Jwt jwt,
                                             @Valid @RequestBody CreateAddressRequest request) {
        CustomerAddressEntity saved = customerAccountService.addAddress(
                jwt.getSubject(),
                request.label(),
                request.fullName(),
                request.phone(),
                request.line1(),
                request.line2(),
                request.city(),
                request.state(),
                request.postalCode(),
                request.country(),
                request.defaultAddress(),
                request.shippingAddress(),
                request.billingAddress()
        );
        return toResponse(saved);
    }

    private static CustomerAddressResponse toResponse(CustomerAddressEntity address) {
        return new CustomerAddressResponse(
                address.getId(),
                address.getLabel(),
                address.getFullName(),
                address.getPhone(),
                address.getLine1(),
                address.getLine2(),
                address.getCity(),
                address.getState(),
                address.getPostalCode(),
                address.getCountry(),
                address.isDefaultAddress(),
                address.isShippingAddress(),
                address.isBillingAddress()
        );
    }

    public record CreateAddressRequest(
            String label,
            String fullName,
            String phone,
            String line1,
            String line2,
            String city,
            String state,
            String postalCode,
            String country,
            boolean defaultAddress,
            boolean shippingAddress,
            boolean billingAddress
    ) {
    }

    public record CustomerAddressResponse(
            Long id,
            String label,
            String fullName,
            String phone,
            String line1,
            String line2,
            String city,
            String state,
            String postalCode,
            String country,
            boolean defaultAddress,
            boolean shippingAddress,
            boolean billingAddress
    ) {
    }
}
