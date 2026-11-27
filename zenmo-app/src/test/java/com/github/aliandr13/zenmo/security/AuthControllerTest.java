package com.github.aliandr13.zenmo.security;

import com.github.aliandr13.zenmo.auth.AuthService;
import com.github.aliandr13.zenmo.auth.InvalidRefreshTokenException;
import com.github.aliandr13.zenmo.auth.RefreshCookieProperties;
import com.github.aliandr13.zenmo.auth.RefreshTokenCookies;
import com.github.aliandr13.zenmo.auth.dto.LoginRequest;
import com.github.aliandr13.zenmo.auth.dto.RefreshRequest;
import com.github.aliandr13.zenmo.auth.dto.RegisterRequest;
import com.github.aliandr13.zenmo.auth.dto.TokensResponse;
import com.github.aliandr13.zenmo.controller.AuthController;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private AuthFacade authFacade;

    private AuthController authController;

    @BeforeEach
    void setUp() {
        RefreshTokenCookies cookies = new RefreshTokenCookies(
                new RefreshCookieProperties("zenmo_refresh", "/api/auth", true),
                new JwtProperties("secret", "issuer", 900, 3600));
        authController = new AuthController(authService, authFacade, cookies);
    }

    private static String setCookie(ResponseEntity<?> response) {
        return response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
    }

    @Test
    void registerReturnsTokensOnValidRequest() {
        TokensResponse tokens = new TokensResponse("access-token", "refresh-token");
        given(authService.register(any(RegisterRequest.class))).willReturn(tokens);

        RegisterRequest request = new RegisterRequest("test@example.com", "password123");

        ResponseEntity<TokensResponse> response = authController.register(request);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().accessToken()).isEqualTo("access-token");
        assertThat(response.getBody().refreshToken()).isEqualTo("refresh-token");
        assertThat(setCookie(response)).startsWith("zenmo_refresh=refresh-token;");
    }

    @Test
    void loginReturnsTokensOnValidCredentials() {
        TokensResponse tokens = new TokensResponse("access-token", "refresh-token");
        given(authService.login(any(LoginRequest.class))).willReturn(tokens);

        LoginRequest request = new LoginRequest("test@example.com", "password123");

        ResponseEntity<TokensResponse> response = authController.login(request);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().accessToken()).isEqualTo("access-token");
        assertThat(response.getBody().refreshToken()).isEqualTo("refresh-token");
    }

    @Test
    void refreshReturnsTokensOnValidRefreshToken() {
        TokensResponse tokens = new TokensResponse("new-access-token", "new-refresh-token");
        given(authService.refresh("refresh")).willReturn(tokens);

        RefreshRequest request = new RefreshRequest("refresh");

        ResponseEntity<TokensResponse> response = authController.refresh(request, null);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().accessToken()).isEqualTo("new-access-token");
        assertThat(response.getBody().refreshToken()).isEqualTo("new-refresh-token");
        assertThat(setCookie(response)).startsWith("zenmo_refresh=new-refresh-token;");
    }

    @Test
    void refreshPrefersBodyTokenOverCookie() {
        given(authService.refresh("from-body")).willReturn(new TokensResponse("a", "r"));

        authController.refresh(new RefreshRequest("from-body"), "from-cookie");

        verify(authService).refresh("from-body");
    }

    @Test
    void refreshUsesCookieWhenBodyMissingOrBlank() {
        given(authService.refresh("from-cookie")).willReturn(new TokensResponse("a", "r"));

        authController.refresh(null, "from-cookie");
        authController.refresh(new RefreshRequest(" "), "from-cookie");

        verify(authService, times(2)).refresh("from-cookie");
    }

    @Test
    void refreshThrowsWhenNoTokenProvided() {
        assertThatThrownBy(() -> authController.refresh(null, " "))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessage("Missing refresh token");
        verifyNoInteractions(authService);
    }

    @Test
    void logoutRevokesTokenAndClearsCookie() {
        ResponseEntity<Void> response = authController.logout(null, "from-cookie");

        verify(authService).logout("from-cookie");
        assertThat(response.getStatusCode().value()).isEqualTo(204);
        assertThat(setCookie(response)).startsWith("zenmo_refresh=;").contains("Max-Age=0");
    }

    @Test
    void logoutWithoutTokenStillClearsCookie() {
        ResponseEntity<Void> response = authController.logout(null, null);

        verifyNoInteractions(authService);
        assertThat(response.getStatusCode().value()).isEqualTo(204);
        assertThat(setCookie(response)).contains("Max-Age=0");
    }

    @Test
    void meReturnsCurrentUser() {
        CurrentUser currentUser = new CurrentUser(UUID.randomUUID(), "me@example.com");
        given(authFacade.currentUser()).willReturn(currentUser);

        ResponseEntity<CurrentUser> response = authController.me();

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().email()).isEqualTo("me@example.com");
    }
}


