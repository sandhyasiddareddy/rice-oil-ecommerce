package com.riceoil.platform.customers.application;

import com.riceoil.platform.customers.domain.CustomerAccountEntity;
import com.riceoil.platform.customers.domain.CustomerAccountRepository;
import com.riceoil.platform.customers.domain.PasswordResetTokenEntity;
import com.riceoil.platform.customers.domain.PasswordResetTokenRepository;
import com.riceoil.platform.notifications.EmailNotificationSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Locale;

@Service
public class PasswordResetService {

    private static final long TOKEN_TTL_MINUTES = 30;
    private final CustomerAccountRepository customerRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailNotificationSender emailNotificationSender;
    private final String frontendBaseUrl;
    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordResetService(CustomerAccountRepository customerRepository,
                                PasswordResetTokenRepository tokenRepository,
                                PasswordEncoder passwordEncoder,
                                EmailNotificationSender emailNotificationSender,
                                @Value("${app.frontend-base-url}") String frontendBaseUrl) {
        this.customerRepository = customerRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailNotificationSender = emailNotificationSender;
        this.frontendBaseUrl = frontendBaseUrl.replaceAll("/+$", "");
    }

    @Transactional
    public void requestReset(String email) {
        if (email == null || email.isBlank()) {
            return;
        }
        CustomerAccountEntity customer = customerRepository.findByEmailIgnoreCase(email.trim())
                .filter(CustomerAccountEntity::isEnabled)
                .orElse(null);
        if (customer == null) {
            return;
        }

        byte[] randomToken = new byte[32];
        secureRandom.nextBytes(randomToken);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomToken);

        tokenRepository.deleteByCustomer_IdAndUsedAtIsNull(customer.getId());
        PasswordResetTokenEntity resetToken = new PasswordResetTokenEntity();
        resetToken.setCustomer(customer);
        resetToken.setTokenHash(hashToken(rawToken));
        resetToken.setExpiresAt(OffsetDateTime.now().plusMinutes(TOKEN_TTL_MINUTES));
        tokenRepository.save(resetToken);

        emailNotificationSender.sendPasswordReset(
                customer.getEmail(),
                frontendBaseUrl + "/reset-password?token=" + rawToken
        );
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        if (rawToken == null || rawToken.isBlank() || newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("A valid reset token and password are required");
        }
        PasswordResetTokenEntity resetToken = tokenRepository
                .findByTokenHashAndUsedAtIsNullAndExpiresAtAfter(hashToken(rawToken), OffsetDateTime.now())
                .orElseThrow(() -> new IllegalArgumentException("The password reset link is invalid or expired"));

        CustomerAccountEntity customer = resetToken.getCustomer();
        customer.setPasswordHash(passwordEncoder.encode(newPassword));
        resetToken.setUsedAt(OffsetDateTime.now());
        tokenRepository.save(resetToken);
    }

    private String hashToken(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest).toLowerCase(Locale.ROOT);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }
}
