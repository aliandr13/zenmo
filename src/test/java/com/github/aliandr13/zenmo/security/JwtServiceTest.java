package com.github.aliandr13.zenmo.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private final JwtProperties props = new JwtProperties(
            "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
            "zenmo-test-issuer",
            60,
            120
    );

    private final JwtService jwtService = new JwtService(props);

    @Test
    void generateAccessTokenAndParseRoundTrip() {
        UUID userId = UUID.randomUUID();
        String email = "user@example.com";

        String token = jwtService.generateAccessToken(userId, email);
        Jws<Claims> parsed = jwtService.parseAndValidate(token);

        assertThat(parsed.getPayload().getSubject()).isEqualTo(userId.toString());
        assertThat(parsed.getPayload().get("email", String.class)).isEqualTo(email);
        assertThat(parsed.getPayload().get("typ", String.class)).isEqualTo("access");
        assertThat(parsed.getPayload().getIssuer()).isEqualTo(props.issuer());
        assertThat(parsed.getPayload().getExpiration()).isAfter(parsed.getPayload().getIssuedAt());
    }
}

