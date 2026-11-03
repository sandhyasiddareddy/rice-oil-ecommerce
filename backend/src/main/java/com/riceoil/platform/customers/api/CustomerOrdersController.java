package com.riceoil.platform.customers.api;

import com.riceoil.platform.customers.application.CustomerOrderQueryService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/customers/me/orders")
public class CustomerOrdersController {

    private final CustomerOrderQueryService customerOrderQueryService;

    public CustomerOrdersController(CustomerOrderQueryService customerOrderQueryService) {
        this.customerOrderQueryService = customerOrderQueryService;
    }

    @GetMapping
    public List<CustomerOrderResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return customerOrderQueryService.listOrdersForCustomer(jwt.getSubject()).stream()
                .map(snapshot -> new CustomerOrderResponse(
                        snapshot.id(),
                        snapshot.orderNumber(),
                        snapshot.status(),
                        snapshot.currency(),
                        snapshot.totalAmount(),
                        snapshot.customerEmail()
                ))
                .toList();
    }

    @GetMapping("/{orderId}")
    public CustomerOrderResponse byId(@AuthenticationPrincipal Jwt jwt,
                                      @PathVariable Long orderId) {
        var snapshot = customerOrderQueryService.getOrderForCustomer(jwt.getSubject(), orderId);
        return new CustomerOrderResponse(
                snapshot.id(),
                snapshot.orderNumber(),
                snapshot.status(),
                snapshot.currency(),
                snapshot.totalAmount(),
                snapshot.customerEmail()
        );
    }
}
