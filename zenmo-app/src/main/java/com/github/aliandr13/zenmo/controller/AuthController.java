package com.github.aliandr13.zenmo.controller;

import com.github.aliandr13.zenmo.auth.AuthService;
import com.github.aliandr13.zenmo.auth.InvalidRefreshTokenException;
import com.github.aliandr13.zenmo.auth.RefreshTokenCookies;
import com.github.aliandr13.zenmo.auth.dto.LoginRequest;
import com.github.aliandr13.zenmo.auth.dto.RefreshRequest;
import com.github.aliandr13.zenmo.auth.dto.RegisterRequest;
import com.github.aliandr13.zenmo.auth.dto.TokensResponse;
import com.github.aliandr13.zenmo.security.AuthFacade;
import com.github.aliandr13.zenmo.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for auth (register, login, refresh, logout, me).
 *
 * <p>Tokens are returned in the JSON body (used by the iOS app). The refresh token is also set as an
 * HttpOnly cookie (used by the web UI, which never stores it in JavaScript-readable storage).
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthFacade authFacade;
    private final RefreshTokenCookies refreshCookies;

    /**
     * Constructor.
     */
    public AuthController(AuthService authService, AuthFacade authFacade, RefreshTokenCookies refreshCookies) {
        this.authService = authService;
        this.authFacade = authFacade;
        this.refreshCookies = refreshCookies;
    }

    /**
     * Registers a new user and returns tokens.
     */
    @PostMapping("/register")
    public ResponseEntity<TokensResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Register request: {}", request.email());
        return withRefreshCookie(authService.register(request));
    }

    /**
     * Logs in and returns tokens.
     */
    @PostMapping("/login")
    public ResponseEntity<TokensResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("login request: {}", request.email());
        return withRefreshCookie(authService.login(request));
    }

    /**
     * Refreshes the access token. The refresh token is taken from the JSON body if present (iOS),
     * otherwise from the HttpOnly cookie (web UI). The refresh token is rotated on every call.
     */
    @PostMapping("/refresh")
    public ResponseEntity<TokensResponse> refresh(
            @RequestBody(required = false) RefreshRequest request,
            @CookieValue(name = "${zenmo.refresh-cookie.name:zenmo_refresh}", required = false) String cookieToken) {
        log.info("Refresh token requested");
        String token = resolveRefreshToken(request, cookieToken);
        if (token == null) {
            throw new InvalidRefreshTokenException("Missing refresh token");
        }
        return withRefreshCookie(authService.refresh(token));
    }

    /**
     * Revokes the refresh token (from body or cookie) and clears the cookie. Always returns 204.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestBody(required = false) RefreshRequest request,
            @CookieValue(name = "${zenmo.refresh-cookie.name:zenmo_refresh}", required = false) String cookieToken) {
        log.info("Logout requested");
        String token = resolveRefreshToken(request, cookieToken);
        if (token != null) {
            authService.logout(token);
        }
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshCookies.clear().toString())
                .build();
    }

    /**
     * Returns the current authenticated user.
     */
    @GetMapping("/me")
    public ResponseEntity<CurrentUser> me() {
        CurrentUser currentUser = authFacade.currentUser();
        log.info("Current user: {}", currentUser.userId());
        return ResponseEntity.ok(currentUser);
    }

    private ResponseEntity<TokensResponse> withRefreshCookie(TokensResponse tokens) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookies.create(tokens.refreshToken()).toString())
                .body(tokens);
    }

    private static String resolveRefreshToken(RefreshRequest request, String cookieToken) {
        if (request != null && request.refreshToken() != null && !request.refreshToken().isBlank()) {
            return request.refreshToken();
        }
        if (cookieToken != null && !cookieToken.isBlank()) {
            return cookieToken;
        }
        return null;
    }
}

