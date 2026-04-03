package com.github.aliandr13.zenmo.service;

import com.github.aliandr13.zenmo.auth.AuthService;
import com.github.aliandr13.zenmo.auth.RefreshToken;
import com.github.aliandr13.zenmo.auth.RefreshTokenRepository;
import com.github.aliandr13.zenmo.auth.dto.LoginRequest;
import com.github.aliandr13.zenmo.auth.dto.RegisterRequest;
import com.github.aliandr13.zenmo.auth.dto.TokensResponse;
import com.github.aliandr13.zenmo.common.NotFoundException;
import com.github.aliandr13.zenmo.security.JwtProperties;
import com.github.aliandr13.zenmo.security.JwtService;
import com.github.aliandr13.zenmo.user.AppUser;
import com.github.aliandr13.zenmo.user.AppUserRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AppUserRepository users;

    @Mock
    private RefreshTokenRepository refreshTokens;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private JwtProperties jwtProperties;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerCreatesUserAndIssuesTokensWhenEmailNotTaken() {
        // GIVEN
        RegisterRequest request = new RegisterRequest("User@Example.com", "password123");
        given(users.existsByEmail("user@example.com")).willReturn(false);
        given(passwordEncoder.encode("password123")).willReturn("encoded");
        given(jwtService.generateAccessToken(any(UUID.class), any(String.class))).willReturn("access-token");

        // WHEN
        TokensResponse tokens = authService.register(request);

        // THEN
        assertThat(tokens.accessToken()).isEqualTo("access-token");
        assertThat(tokens.refreshToken()).isNotBlank();

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(users).save(captor.capture());
        AppUser created = captor.getValue();
        assertThat(created.getEmail()).isEqualTo("user@example.com");
        assertThat(created.getPasswordHash()).isEqualTo("encoded");
    }

    @Test
    void registerThrowsWhenEmailAlreadyRegistered() {
        RegisterRequest request = new RegisterRequest("user@example.com", "password123");
        given(users.existsByEmail("user@example.com")).willReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already registered");

        verify(users, never()).save(any());
        verifyNoInteractions(jwtService, refreshTokens);
    }

    @Test
    void loginAuthenticatesAndIssuesTokensOnValidCredentials() {
        LoginRequest request = new LoginRequest("user@example.com", "password123");
        Authentication auth = new UsernamePasswordAuthenticationToken("user@example.com", "password123");
        given(authenticationManager.authenticate(any(Authentication.class))).willReturn(auth);

        AppUser user = new AppUser(UUID.randomUUID(), "user@example.com", "hashed", Instant.now());
        given(users.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(jwtService.generateAccessToken(user.getId(), user.getEmail())).willReturn("access-token");

        TokensResponse tokens = authService.login(request);

        assertThat(tokens.accessToken()).isEqualTo("access-token");
        assertThat(tokens.refreshToken()).isNotBlank();
    }

    @Test
    void loginThrowsWhenUserNotFound() {
        LoginRequest request = new LoginRequest("missing@example.com", "password123");
        Authentication auth = new UsernamePasswordAuthenticationToken("missing@example.com", "password123");
        given(authenticationManager.authenticate(any(Authentication.class))).willReturn(auth);
        given(users.findByEmail("missing@example.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void refreshIssuesNewTokensWhenRefreshTokenValid() {
        UUID userId = UUID.randomUUID();
        RefreshToken stored = new RefreshToken(UUID.randomUUID(), userId, "hash", Instant.now().plusSeconds(3600), null, Instant.now());
        given(refreshTokens.findByTokenHash(any(String.class))).willReturn(Optional.of(stored));

        given(jwtProperties.refreshTtlSeconds()).willReturn(3600L);

        AppUser user = new AppUser(userId, "user@example.com", "hashed", Instant.now());
        given(users.findById(userId)).willReturn(Optional.of(user));
        given(jwtService.generateAccessToken(userId, "user@example.com")).willReturn("access-token");

        TokensResponse tokens = authService.refresh("raw-refresh-token");

        assertThat(tokens.accessToken()).isEqualTo("access-token");
        assertThat(tokens.refreshToken()).isNotBlank();
        verify(refreshTokens).delete(eq(stored));
    }

    @Test
    void refreshThrowsWhenTokenNotFound() {
        given(refreshTokens.findByTokenHash(any(String.class))).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh("raw-refresh-token"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid refresh token");
    }
}

