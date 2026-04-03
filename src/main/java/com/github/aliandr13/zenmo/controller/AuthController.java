package com.github.aliandr13.zenmo.controller;

import com.github.aliandr13.zenmo.auth.AuthService;
import com.github.aliandr13.zenmo.auth.dto.LoginRequest;
import com.github.aliandr13.zenmo.auth.dto.RefreshRequest;
import com.github.aliandr13.zenmo.auth.dto.RegisterRequest;
import com.github.aliandr13.zenmo.auth.dto.TokensResponse;
import com.github.aliandr13.zenmo.security.AuthFacade;
import com.github.aliandr13.zenmo.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for auth (register, login, refresh, me).
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthFacade authFacade;

    /**
     * Constructor.
     */
    public AuthController(AuthService authService, AuthFacade authFacade) {
        this.authService = authService;
        this.authFacade = authFacade;
    }

    /**
     * Registers a new user and returns tokens.
     */
    @PostMapping("/register")
    public ResponseEntity<TokensResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Register request: {}", request.email());
        return ResponseEntity.ok(authService.register(request));
    }

    /**
     * Logs in and returns tokens.
     */
    @PostMapping("/login")
    public ResponseEntity<TokensResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("login request: {}", request.email());
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * Refreshes access token using refresh token.
     */
    @PostMapping("/refresh")
    public ResponseEntity<TokensResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        log.info("Refresh token requested");
        return ResponseEntity.ok(authService.refresh(request.refreshToken()));
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
}

