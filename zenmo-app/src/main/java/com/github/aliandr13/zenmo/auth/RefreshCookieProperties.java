package com.github.aliandr13.zenmo.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings for the HttpOnly refresh-token cookie used by the web UI.
 *
 * @param name   cookie name
 * @param path   cookie path; limits the cookie to the auth endpoints
 * @param secure whether to set the {@code Secure} attribute (disable only for plain-HTTP local dev)
 */
@ConfigurationProperties(prefix = "zenmo.refresh-cookie")
public record RefreshCookieProperties(
        @DefaultValue("zenmo_refresh") String name,
        @DefaultValue("/api/auth") String path,
        @DefaultValue("true") boolean secure
) {
}
