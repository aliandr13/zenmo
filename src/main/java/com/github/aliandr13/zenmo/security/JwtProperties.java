package com.github.aliandr13.zenmo.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT configuration properties.
 */
@ConfigurationProperties(prefix = "zenmo.jwt")
public record JwtProperties(
        String secret,
        String issuer,
        long accessTtlSeconds,
        long refreshTtlSeconds
) {
}

