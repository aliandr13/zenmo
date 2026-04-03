package com.github.aliandr13.zenmo.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/**
 * Service for generating and validating JWT tokens.
 */
@Service
public class JwtService {

    private final JwtProperties props;
    private final SecretKey signingKey;

    /**
     * Constructor.
     */
    public JwtService(JwtProperties props) {
        this.props = props;
        this.signingKey = Keys.hmacShaKeyFor(props.secret().getBytes());
    }

    /**
     * Builds a signed access JWT for the given user.
     */
    public String generateAccessToken(UUID userId, String email) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(props.accessTtlSeconds());

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(userId.toString())
                .issuer(props.issuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .claims(Map.of("email", email, "typ", "access"))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Parses and validates a JWT, returning its claims.
     */
    public Jws<Claims> parseAndValidate(String token) {
        return Jwts.parser()
                .requireIssuer(props.issuer())
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token);
    }
}

