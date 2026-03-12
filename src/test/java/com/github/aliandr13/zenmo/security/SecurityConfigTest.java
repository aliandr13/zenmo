package com.github.aliandr13.zenmo.security;

import com.github.aliandr13.zenmo.user.AppUser;
import com.github.aliandr13.zenmo.user.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class SecurityConfigTest {

    private final JwtAuthenticationFilter jwtFilter = mock(JwtAuthenticationFilter.class);
    private final SecurityConfig securityConfig = new SecurityConfig(jwtFilter);

    @Test
    void userDetailsServiceLoadsUserAndWrapsInUserPrincipal() {
        AppUserRepository repo = mock(AppUserRepository.class);
        UUID userId = UUID.randomUUID();
        AppUser user = new AppUser(userId, "user@example.com", "hashed", Instant.now());
        given(repo.findByEmailIgnoreCase("user@example.com")).willReturn(Optional.of(user));

        var uds = securityConfig.userDetailsService(repo);
        UserDetails details = uds.loadUserByUsername("user@example.com");

        assertThat(details).isInstanceOf(UserPrincipal.class);
        UserPrincipal principal = (UserPrincipal) details;
        assertThat(principal.userId()).isEqualTo(userId);
        assertThat(principal.email()).isEqualTo("user@example.com");
        assertThat(principal.getPassword()).isEqualTo("hashed");
    }

    @Test
    void userDetailsServiceThrowsWhenUserNotFound() {
        AppUserRepository repo = mock(AppUserRepository.class);
        given(repo.findByEmailIgnoreCase("missing@example.com")).willReturn(Optional.empty());

        var uds = securityConfig.userDetailsService(repo);

        assertThatThrownBy(() -> uds.loadUserByUsername("missing@example.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void passwordEncoderIsBcrypt() {
        PasswordEncoder encoder = securityConfig.passwordEncoder();
        String encoded = encoder.encode("password");

        assertThat(encoded).isNotBlank();
        assertThat(encoder.matches("password", encoded)).isTrue();
    }
}

