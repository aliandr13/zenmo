package com.github.aliandr13.zenmo.auth;

import com.github.aliandr13.zenmo.auth.dto.LoginRequest;
import com.github.aliandr13.zenmo.auth.dto.RegisterRequest;
import com.github.aliandr13.zenmo.auth.dto.TokensResponse;
import com.github.aliandr13.zenmo.common.NotFoundException;
import com.github.aliandr13.zenmo.security.JwtProperties;
import com.github.aliandr13.zenmo.security.JwtService;
import com.github.aliandr13.zenmo.user.AppUser;
import com.github.aliandr13.zenmo.user.AppUserRepository;
import com.github.aliandr13.zenmo.utils.StringUtils;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for registration, login, refresh tokens and JWT issuance.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Registers a new user and returns tokens.
     */
    @Transactional
    public TokensResponse register(RegisterRequest request) {
        if (users.existsByEmail(StringUtils.normalize(request.email()))) {
            throw new IllegalArgumentException("Email already registered");
        }

        AppUser user = new AppUser(UUID.randomUUID(), StringUtils.normalize(request.email()),
                passwordEncoder.encode(request.password()), Instant.now());
        users.save(user);

        return issueTokens(user);
    }

    /**
     * Authenticates and returns tokens.
     */
    @Transactional
    public TokensResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        StringUtils.normalize(request.email()),
                        request.password()
                )
        );

        String email = auth.getName();
        AppUser user = users.findByEmail(email).orElseThrow(() -> new NotFoundException("User not found"));
        return issueTokens(user);
    }

    /**
     * Issues new tokens using a valid refresh token.
     */
    @Transactional
    public TokensResponse refresh(String refreshToken) {
        String hash = hashToken(refreshToken);
        RefreshToken stored = refreshTokens.findByTokenHash(hash)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        if (stored.isRevoked() || stored.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Refresh token expired or revoked");
        }

        AppUser user = users.findById(stored.getUserId()).orElseThrow(() -> new NotFoundException("User not found"));

        // Rotate token: delete old and issue new
        refreshTokens.delete(stored);
        return issueTokens(user);
    }

    private TokensResponse issueTokens(AppUser user) {
        String access = jwtService.generateAccessToken(user.getId(), user.getEmail());
        String refresh = createRefreshToken(user.getId());
        return new TokensResponse(access, refresh);
    }

    private String createRefreshToken(UUID userId) {
        byte[] bytes = new byte[64];
        secureRandom.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        String hash = hashToken(token);
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(jwtProperties.refreshTtlSeconds());

        RefreshToken entity = new RefreshToken(UUID.randomUUID(), userId, hash, exp, null, now);
        refreshTokens.save(entity);
        return token;
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hashBytes.length * 2);
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}

