package com.riceoil.platform.customers.application;

import com.riceoil.platform.customers.domain.CustomerAccountEntity;
import com.riceoil.platform.customers.domain.CustomerAccountRepository;
import com.riceoil.platform.customers.domain.PasswordResetTokenEntity;
import com.riceoil.platform.customers.domain.PasswordResetTokenRepository;
import com.riceoil.platform.notifications.EmailNotificationSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private CustomerAccountRepository customerRepository;

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailNotificationSender emailNotificationSender;

    private PasswordResetService passwordResetService;

    @BeforeEach
    void setUp() {
        passwordResetService = new PasswordResetService(
                customerRepository,
                tokenRepository,
                passwordEncoder,
                emailNotificationSender,
                "http://localhost:3000"
        );
    }

    @Test
    void requestReset_sendsUrlAndPersistsOnlyTokenHash() throws Exception {
        CustomerAccountEntity customer = customer();
        when(customerRepository.findByEmailIgnoreCase("Test@example.com")).thenReturn(Optional.of(customer));
        when(tokenRepository.save(any(PasswordResetTokenEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        passwordResetService.requestReset(" Test@example.com ");

        ArgumentCaptor<PasswordResetTokenEntity> tokenCaptor = ArgumentCaptor.forClass(PasswordResetTokenEntity.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        PasswordResetTokenEntity savedToken = tokenCaptor.getValue();
        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailNotificationSender).sendPasswordReset(eq("test@example.com"), urlCaptor.capture());
        String rawToken = urlCaptor.getValue().substring(urlCaptor.getValue().indexOf("?token=") + 7);
        assertNotEquals(rawToken, savedToken.getTokenHash());
        assertEquals(64, savedToken.getTokenHash().length());
        assertExpiryWithinThirtyMinutes(savedToken.getExpiresAt());
    }

    @Test
    void requestReset_doesNotRevealUnknownAccountOrSendMail() {
        when(customerRepository.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());

        passwordResetService.requestReset("missing@example.com");

        verify(emailNotificationSender, never()).sendPasswordReset(anyString(), anyString());
    }

    @Test
    void resetPassword_updatesPasswordAndConsumesToken() throws Exception {
        CustomerAccountEntity customer = customer();
        PasswordResetTokenEntity token = new PasswordResetTokenEntity();
        token.setCustomer(customer);
        token.setTokenHash("stored-hash");
        token.setExpiresAt(OffsetDateTime.now().plusMinutes(20));
        when(tokenRepository.findByTokenHashAndUsedAtIsNullAndExpiresAtAfter(eq(hashToken("raw-token")), any()))
                .thenReturn(Optional.of(token));
        when(passwordEncoder.encode("new-password")).thenReturn("encoded-password");

        passwordResetService.resetPassword("raw-token", "new-password");

        assertEquals("encoded-password", customer.getPasswordHash());
        assertNotNull(token.getUsedAt());
        verify(tokenRepository).save(token);
    }

    @Test
    void resetPassword_rejectsShortPassword() {
        assertThrows(IllegalArgumentException.class,
                () -> passwordResetService.resetPassword("raw-token", "short"));
        verify(tokenRepository, never()).findByTokenHashAndUsedAtIsNullAndExpiresAtAfter(anyString(), any());
    }

    private static CustomerAccountEntity customer() throws Exception {
        CustomerAccountEntity customer = new CustomerAccountEntity();
        Field id = CustomerAccountEntity.class.getDeclaredField("id");
        id.setAccessible(true);
        id.set(customer, 42L);
        customer.setEmail("test@example.com");
        customer.setFullName("Test User");
        customer.setEnabled(true);
        return customer;
    }

    private static void assertExpiryWithinThirtyMinutes(OffsetDateTime expiresAt) {
        assertFalse(expiresAt.isBefore(OffsetDateTime.now().plusMinutes(29)));
        assertFalse(expiresAt.isAfter(OffsetDateTime.now().plusMinutes(31)));
    }

    private static String hashToken(String rawToken) throws Exception {
        return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(rawToken.getBytes(StandardCharsets.UTF_8)));
    }
}
