package com.github.aliandr13.zenmo.auth;

import com.github.aliandr13.zenmo.security.JwtProperties;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Builds the HttpOnly refresh-token cookie. JavaScript cannot read it, so an XSS bug cannot steal the
 * long-lived refresh token. {@code SameSite=Strict} keeps other sites from sending it.
 */
@Component
@RequiredArgsConstructor
public class RefreshTokenCookies {

    private final RefreshCookieProperties cookieProperties;
    private final JwtProperties jwtProperties;

    /**
     * Returns the cookie name.
     */
    public String name() {
        return cookieProperties.name();
    }

    /**
     * Cookie carrying the given refresh token, valid for the refresh token TTL.
     */
    public ResponseCookie create(String refreshToken) {
        return base(refreshToken)
                .maxAge(Duration.ofSeconds(jwtProperties.refreshTtlSeconds()))
                .build();
    }

    /**
     * Cookie that tells the browser to delete the refresh-token cookie.
     */
    public ResponseCookie clear() {
        return base("").maxAge(Duration.ZERO).build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(cookieProperties.name(), value)
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .sameSite("Strict")
                .path(cookieProperties.path());
    }
}
