package com.github.aliandr13.zenmo.security;

import com.github.aliandr13.zenmo.auth.AuthService;
import com.github.aliandr13.zenmo.auth.dto.LoginRequest;
import com.github.aliandr13.zenmo.auth.dto.RefreshRequest;
import com.github.aliandr13.zenmo.auth.dto.RegisterRequest;
import com.github.aliandr13.zenmo.auth.dto.TokensResponse;
import com.github.aliandr13.zenmo.controller.AuthController;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private AuthFacade authFacade;

    @InjectMocks
    private AuthController authController;

    @Test
    void registerReturnsTokensOnValidRequest() {
        TokensResponse tokens = new TokensResponse("access-token", "refresh-token");
        given(authService.register(any(RegisterRequest.class))).willReturn(tokens);

        RegisterRequest request = new RegisterRequest("test@example.com", "password123");

        ResponseEntity<TokensResponse> response = authController.register(request);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().accessToken()).isEqualTo("access-token");
        assertThat(response.getBody().refreshToken()).isEqualTo("refresh-token");
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

        ResponseEntity<TokensResponse> response = authController.refresh(request);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().accessToken()).isEqualTo("new-access-token");
        assertThat(response.getBody().refreshToken()).isEqualTo("new-refresh-token");
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


