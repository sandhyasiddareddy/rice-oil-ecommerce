package com.riceoil.platform.customers.api;

import com.riceoil.platform.customers.application.CustomerAccountService;
import com.riceoil.platform.customers.domain.CustomerAddressEntity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;

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
        return toResponse(customerAccountService.addAddress(
                jwt.getSubject(), request.label(), request.fullName(), request.phone(), request.line1(),
                request.line2(), request.city(), request.state(), request.postalCode(), request.country(),
                request.defaultAddress(), request.shippingAddress(), request.billingAddress()));
    }

    @PutMapping("/addresses/{addressId}")
    public CustomerAddressResponse updateAddress(@AuthenticationPrincipal Jwt jwt,
                                                 @PathVariable Long addressId,
                                                 @Valid @RequestBody CreateAddressRequest request) {
        return toResponse(customerAccountService.updateAddress(
                jwt.getSubject(), addressId, request.label(), request.fullName(), request.phone(), request.line1(),
                request.line2(), request.city(), request.state(), request.postalCode(), request.country(),
                request.defaultAddress(), request.shippingAddress(), request.billingAddress()));
    }

    @DeleteMapping("/addresses/{addressId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAddress(@AuthenticationPrincipal Jwt jwt, @PathVariable Long addressId) {
        customerAccountService.deleteAddress(jwt.getSubject(), addressId);
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
            @NotBlank
            String fullName,
            @NotBlank
            String phone,
            @NotBlank
            String line1,
            String line2,
            @NotBlank
            String city,
            @NotBlank
            String state,
            @NotBlank
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
