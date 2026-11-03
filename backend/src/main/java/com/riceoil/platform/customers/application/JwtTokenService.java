package com.riceoil.platform.customers.application;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.riceoil.platform.customers.domain.CustomerAccountEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

@Service
public class JwtTokenService {

    private final String issuer;
    private final long ttlMinutes;
    private final String secret;

    public JwtTokenService(@Value("${app.security.jwt.issuer:farmora}") String issuer,
                          @Value("${app.security.jwt.ttl-minutes:120}") long ttlMinutes,
                          @Value("${app.security.jwt.secret:change-me-please-local-dev-secret}") String secret) {
        this.issuer = issuer;
        this.ttlMinutes = ttlMinutes;
        this.secret = secret;
    }

    public String generateToken(CustomerAccountEntity customer) {
        try {
            Instant issuedAt = Instant.now();
            Instant expiration = issuedAt.plusSeconds(ttlMinutes * 60L);
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .issuer(issuer)
                    .subject(customer.getEmail())
                    .claim("email", customer.getEmail())
                    .claim("customerId", customer.getId())
                    .claim("fullName", customer.getFullName())
                    .claim("roles", List.of(customer.getRole().replace("ROLE_", "")))
                    .issueTime(java.util.Date.from(issuedAt))
                    .expirationTime(java.util.Date.from(expiration))
                    .build();

            SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            signedJWT.sign(new MACSigner(secret.getBytes(StandardCharsets.UTF_8)));
            return signedJWT.serialize();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to sign JWT", ex);
        }
    }
}
