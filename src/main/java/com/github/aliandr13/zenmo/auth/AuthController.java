package com.github.aliandr13.zenmo.auth;

import com.github.aliandr13.zenmo.auth.dto.LoginRequest;
import com.github.aliandr13.zenmo.auth.dto.RefreshRequest;
import com.github.aliandr13.zenmo.auth.dto.RegisterRequest;
import com.github.aliandr13.zenmo.auth.dto.TokensResponse;
import com.github.aliandr13.zenmo.security.AuthFacade;
import com.github.aliandr13.zenmo.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthFacade authFacade;

    public AuthController(AuthService authService, AuthFacade authFacade) {
        this.authService = authService;
        this.authFacade = authFacade;
    }

    @PostMapping("/register")
    public ResponseEntity<TokensResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Register request: {}", request.email());
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<TokensResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("login request: {}", request.email());
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokensResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        log.info("Refresh token requested");
        return ResponseEntity.ok(authService.refresh(request.refreshToken()));
    }

    @GetMapping("/me")
    public ResponseEntity<CurrentUser> me() {
        CurrentUser currentUser = authFacade.currentUser();
        log.info("Current user: {}", currentUser.userId());
        return ResponseEntity.ok(currentUser);
    }
}

