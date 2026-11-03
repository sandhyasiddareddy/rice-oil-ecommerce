package com.riceoil.platform.customers.api;

import com.riceoil.platform.customers.application.CustomerAccountService;
import com.riceoil.platform.customers.application.JwtTokenService;
import com.riceoil.platform.customers.domain.CustomerAccountEntity;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final CustomerAccountService customerAccountService;
    private final JwtTokenService jwtTokenService;

    public AuthController(CustomerAccountService customerAccountService,
                          JwtTokenService jwtTokenService) {
        this.customerAccountService = customerAccountService;
        this.jwtTokenService = jwtTokenService;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        CustomerAccountEntity customer = customerAccountService.register(
                request.fullName(),
                request.email(),
                request.password()
        );
        return new AuthResponse(jwtTokenService.generateToken(customer), customer.getEmail(), customer.getFullName(), customer.getRole());
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        CustomerAccountEntity customer = customerAccountService.authenticate(request.email(), request.password());
        return new AuthResponse(jwtTokenService.generateToken(customer), customer.getEmail(), customer.getFullName(), customer.getRole());
    }

    public record RegisterRequest(String fullName, String email, String password) {
    }

    public record LoginRequest(String email, String password) {
    }

    public record AuthResponse(String token, String email, String fullName, String role) {
    }
}
