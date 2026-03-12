package com.github.aliandr13.zenmo.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthFacadeTest {

    private final AuthFacade authFacade = new AuthFacade();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void currentUserReturnsCurrentUserFromUserPrincipal() {
        UUID userId = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(userId, "user@example.com", "hashed");
        TestingAuthenticationToken auth = new TestingAuthenticationToken(principal, null);
        SecurityContextHolder.getContext().setAuthentication(auth);

        CurrentUser current = authFacade.currentUser();

        assertThat(current.userId()).isEqualTo(userId);
        assertThat(current.email()).isEqualTo("user@example.com");
    }

    @Test
    void currentUserThrowsWhenNoAuthentication() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(authFacade::currentUser)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No authenticated user in context");
    }

    @Test
    void currentUserThrowsWhenUnexpectedPrincipalType() {
        TestingAuthenticationToken auth = new TestingAuthenticationToken("string-principal", null);
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThatThrownBy(authFacade::currentUser)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unexpected principal type");
    }
}

