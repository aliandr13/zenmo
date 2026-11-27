package com.github.aliandr13.zenmo.auth;

import com.github.aliandr13.zenmo.security.JwtProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;
import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenCookiesTest {

    private static RefreshTokenCookies cookies(boolean secure) {
        return new RefreshTokenCookies(
                new RefreshCookieProperties("zenmo_refresh", "/api/auth", secure),
                new JwtProperties("secret", "issuer", 900, 3600));
    }

    @Test
    void createSetsHardenedAttributesAndRefreshTtl() {
        ResponseCookie cookie = cookies(true).create("tok");

        assertThat(cookie.getName()).isEqualTo("zenmo_refresh");
        assertThat(cookie.getValue()).isEqualTo("tok");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isTrue();
        assertThat(cookie.getSameSite()).isEqualTo("Strict");
        assertThat(cookie.getPath()).isEqualTo("/api/auth");
        assertThat(cookie.getMaxAge().getSeconds()).isEqualTo(3600);
    }

    @Test
    void secureFlagFollowsProperties() {
        assertThat(cookies(false).create("tok").isSecure()).isFalse();
    }

    @Test
    void clearExpiresCookieImmediately() {
        RefreshTokenCookies c = cookies(true);
        ResponseCookie cookie = c.clear();

        assertThat(c.name()).isEqualTo("zenmo_refresh");
        assertThat(cookie.getValue()).isEmpty();
        assertThat(cookie.getMaxAge().isZero()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/api/auth");
        assertThat(cookie.isHttpOnly()).isTrue();
    }
}
