package com.helpflow.supportticket.infrastructure.adapter.out.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * JWT settings. The signing secret must come from the environment ({@code JWT_SECRET}); the
 * application refuses to start without a secret of at least 32 characters (256 bits for HS256).
 */
@Validated
@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(
        @NotBlank @Size(min = 32, message = "JWT_SECRET must have at least 32 characters") String secret,
        @NotBlank String issuer,
        Duration expiration) {

    public JwtProperties {
        expiration = expiration == null ? Duration.ofHours(1) : expiration;
    }
}
