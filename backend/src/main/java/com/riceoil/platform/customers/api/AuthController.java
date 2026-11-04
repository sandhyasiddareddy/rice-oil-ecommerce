package com.riceoil.platform.customers.api;

import com.riceoil.platform.customers.application.CustomerAccountService;
import com.riceoil.platform.customers.application.InvalidCredentialsException;
import com.riceoil.platform.customers.application.JwtTokenService;
import com.riceoil.platform.shared.observability.BusinessLog;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import java.util.UUID;
import com.riceoil.platform.customers.application.PasswordResetService;
import com.riceoil.platform.customers.domain.CustomerAccountEntity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final CustomerAccountService customerAccountService;
    private final JwtTokenService jwtTokenService;
    private final PasswordResetService passwordResetService;

    public AuthController(CustomerAccountService customerAccountService,
                          JwtTokenService jwtTokenService,
                          PasswordResetService passwordResetService) {
        this.customerAccountService = customerAccountService;
        this.jwtTokenService = jwtTokenService;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        CustomerAccountEntity customer = customerAccountService.register(
                request.fullName(),
                request.email(),
                request.password()
        );
        String sessionId = UUID.randomUUID().toString();
        BusinessLog.info("CUSTOMER_REGISTERED", "customerId", customer.getId(),
                "email", BusinessLog.maskEmail(customer.getEmail()), "sessionId", sessionId);
        return new AuthResponse(jwtTokenService.generateToken(customer, sessionId), customer.getEmail(), customer.getFullName(), customer.getRole());
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        CustomerAccountEntity customer;
        try {
            customer = customerAccountService.authenticate(request.email(), request.password());
        } catch (InvalidCredentialsException ex) {
            BusinessLog.warn("LOGIN_FAILED", "email", BusinessLog.maskEmail(request.email()), "reason", "invalid_credentials");
            throw ex;
        }
        String sessionId = UUID.randomUUID().toString();
        BusinessLog.info("LOGIN_SUCCESS", "customerId", customer.getId(),
                "email", BusinessLog.maskEmail(customer.getEmail()), "sessionId", sessionId);
        return new AuthResponse(jwtTokenService.generateToken(customer, sessionId), customer.getEmail(), customer.getFullName(), customer.getRole());
    }

    /** Tokens are stateless; this endpoint exists so the end of a customer session appears in the logs. */
    @PostMapping("/logout")
    public MessageResponse logout(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            BusinessLog.info("LOGOUT", "authenticated", false);
        } else {
            BusinessLog.info("LOGOUT", "customerId", jwt.getClaims().get("customerId"), "sessionId", jwt.getId());
        }
        return new MessageResponse("You have been signed out.");
    }

    @PostMapping("/password-reset/request")
    public MessageResponse requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        passwordResetService.requestReset(request.email());
        return new MessageResponse("If an account exists for that email, a password reset link will be sent.");
    }

    @PostMapping("/password-reset/complete")
    public MessageResponse completePasswordReset(@Valid @RequestBody CompletePasswordResetRequest request) {
        passwordResetService.resetPassword(request.token(), request.newPassword());
        return new MessageResponse("Your password has been reset. You can now sign in.");
    }

    public record RegisterRequest(@NotBlank @Size(max = 160) String fullName,
                                  @NotBlank @Email @Size(max = 160) String email,
                                  @NotBlank @Size(min = 8, max = 72) String password) {
    }

    public record LoginRequest(@NotBlank @Email @Size(max = 160) String email,
                               @NotBlank String password) {
    }

    public record AuthResponse(String token, String email, String fullName, String role) {
    }

    public record PasswordResetRequest(@NotBlank @Email @Size(max = 160) String email) {
    }

    public record CompletePasswordResetRequest(@NotBlank String token,
                                               @NotBlank @Size(min = 8, max = 72) String newPassword) {
    }

    public record MessageResponse(String message) {
    }
}
