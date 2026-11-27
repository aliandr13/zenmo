package com.github.aliandr13.zenmo.integration;

import com.github.aliandr13.zenmo.auth.dto.LoginRequest;
import com.github.aliandr13.zenmo.auth.dto.RefreshRequest;
import com.github.aliandr13.zenmo.auth.dto.RegisterRequest;
import com.github.aliandr13.zenmo.auth.dto.TokensResponse;
import com.github.aliandr13.zenmo.security.CurrentUser;
import com.github.aliandr13.zenmo.security.JwtProperties;
import com.github.aliandr13.zenmo.security.JwtService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthControllerIT {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String COOKIE = "zenmo_refresh";

    @LocalServerPort
    private int port;

    @Autowired
    private JwtProperties jwtProperties;

    @Test
    void registerLoginAndMeFlowWorks() {
        RestClient client = IntegrationTestRestClient.create();

        // register
        RegisterRequest registerRequest = new RegisterRequest("test@example.com", "password123");
        TokensResponse registerTokens = client.post()
                .uri("http://localhost:" + port + "/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(registerRequest)
                .retrieve()
                .body(TokensResponse.class);

        assertThat(registerTokens).isNotNull();
        assertThat(registerTokens.accessToken()).isNotBlank();
        assertThat(registerTokens.refreshToken()).isNotBlank();

        // login
        LoginRequest loginRequest = new LoginRequest("test@example.com", "password123");
        TokensResponse loginTokens = client.post()
                .uri("http://localhost:" + port + "/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(loginRequest)
                .retrieve()
                .body(TokensResponse.class);

        assertThat(loginTokens).isNotNull();
        assertThat(loginTokens.accessToken()).isNotBlank();

        // call /me with access token
        CurrentUser me = client.get()
                .uri("http://localhost:" + port + "/api/auth/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + loginTokens.accessToken())
                .retrieve()
                .body(CurrentUser.class);

        assertThat(me).isNotNull();
        assertThat(me.email()).isEqualTo("test@example.com");
    }

    @Test
    void loginWithoutRegistrationReturns401InvalidCredentials() {
        assertLoginReturns401InvalidCredentials(new LoginRequest("notregistered@example.com", "password123"));
    }

    @Test
    void loginWithWrongPasswordReturns401InvalidCredentials() {
        RestClient client = IntegrationTestRestClient.create();
        RegisterRequest registerRequest = new RegisterRequest("wrongpwd-user@example.com", "correct-secret");
        client.post()
                .uri("http://localhost:" + port + "/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(registerRequest)
                .retrieve()
                .body(TokensResponse.class);

        assertLoginReturns401InvalidCredentials(
                new LoginRequest("wrongpwd-user@example.com", "wrong-secret"));
    }

    @Test
    void protectedEndpointWithoutTokenReturns401Json() {
        assertAccountsReturns401(null);
    }

    @Test
    void protectedEndpointWithGarbageTokenReturns401Json() {
        assertAccountsReturns401("not-a-jwt");
    }

    @Test
    void protectedEndpointWithExpiredTokenReturns401Json() {
        JwtService expiredIssuer = new JwtService(new JwtProperties(
                jwtProperties.secret(), jwtProperties.issuer(), -60, jwtProperties.refreshTtlSeconds()));
        assertAccountsReturns401(expiredIssuer.generateAccessToken(UUID.randomUUID(), "expired@example.com"));
    }

    @Test
    void loginSetsHttpOnlyRefreshCookieMatchingBodyToken() {
        ResponseEntity<TokensResponse> response = register("cookie-attrs@example.com");

        String setCookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(setCookie)
                .startsWith(COOKIE + "=" + response.getBody().refreshToken() + ";")
                .contains("HttpOnly", "SameSite=Strict", "Path=/api/auth", "Secure", "Max-Age=2592000");
    }

    @Test
    void refreshWithCookieOnlyRotatesTokenAndOldCookieIsRejected() {
        String oldToken = register("cookie-refresh@example.com").getBody().refreshToken();

        ResponseEntity<TokensResponse> refreshed = client().post()
                .uri(url("/api/auth/refresh"))
                .header(HttpHeaders.COOKIE, COOKIE + "=" + oldToken)
                .retrieve()
                .toEntity(TokensResponse.class);

        TokensResponse tokens = refreshed.getBody();
        assertThat(tokens).isNotNull();
        assertThat(tokens.refreshToken()).isNotEqualTo(oldToken);
        assertThat(refreshed.getHeaders().getFirst(HttpHeaders.SET_COOKIE))
                .startsWith(COOKIE + "=" + tokens.refreshToken() + ";");
        CurrentUser me = client().get()
                .uri(url("/api/auth/me"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.accessToken())
                .retrieve()
                .body(CurrentUser.class);
        assertThat(me.email()).isEqualTo("cookie-refresh@example.com");

        assertRefreshWithCookieReturns401(oldToken, "Invalid refresh token");
    }

    @Test
    void refreshWithBodyStillWorksForNativeClients() {
        String oldToken = register("body-refresh@example.com").getBody().refreshToken();

        TokensResponse tokens = client().post()
                .uri(url("/api/auth/refresh"))
                .contentType(MediaType.APPLICATION_JSON)
                .body(new RefreshRequest(oldToken))
                .retrieve()
                .body(TokensResponse.class);

        assertThat(tokens).isNotNull();
        assertThat(tokens.accessToken()).isNotBlank();
        assertThat(tokens.refreshToken()).isNotEqualTo(oldToken);
    }

    @Test
    void refreshWithoutAnyTokenReturns401() {
        assertThatThrownBy(() -> client().post()
                .uri(url("/api/auth/refresh"))
                .retrieve()
                .body(TokensResponse.class))
                .isInstanceOfSatisfying(RestClientResponseException.class, ex -> {
                    assertThat(ex.getStatusCode().value()).isEqualTo(401);
                    assertThat(errorMessageFromBody(ex)).isEqualTo("Missing refresh token");
                });
    }

    @Test
    void logoutRevokesRefreshTokenAndClearsCookie() {
        String token = register("logout@example.com").getBody().refreshToken();

        ResponseEntity<Void> response = client().post()
                .uri(url("/api/auth/logout"))
                .header(HttpHeaders.COOKIE, COOKIE + "=" + token)
                .retrieve()
                .toBodilessEntity();

        assertThat(response.getStatusCode().value()).isEqualTo(204);
        assertThat(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE))
                .startsWith(COOKIE + "=;")
                .contains("Max-Age=0", "Path=/api/auth");
        assertRefreshWithCookieReturns401(token, "Invalid refresh token");
    }

    private ResponseEntity<TokensResponse> register(String email) {
        return client().post()
                .uri(url("/api/auth/register"))
                .contentType(MediaType.APPLICATION_JSON)
                .body(new RegisterRequest(email, "password123"))
                .retrieve()
                .toEntity(TokensResponse.class);
    }

    private void assertAccountsReturns401(String accessToken) {
        assertThatThrownBy(() -> client().get()
                .uri(url("/api/accounts"))
                .headers(h -> {
                    if (accessToken != null) {
                        h.setBearerAuth(accessToken);
                    }
                })
                .retrieve()
                .body(String.class))
                .isInstanceOfSatisfying(RestClientResponseException.class, ex -> {
                    assertThat(ex.getStatusCode().value()).isEqualTo(401);
                    assertThat(ex.getResponseHeaders().getContentType().isCompatibleWith(MediaType.APPLICATION_JSON))
                            .isTrue();
                    assertThat(errorMessageFromBody(ex)).isEqualTo("Unauthorized");
                });
    }

    private void assertRefreshWithCookieReturns401(String token, String expectedMessage) {
        assertThatThrownBy(() -> client().post()
                .uri(url("/api/auth/refresh"))
                .header(HttpHeaders.COOKIE, COOKIE + "=" + token)
                .retrieve()
                .body(TokensResponse.class))
                .isInstanceOfSatisfying(RestClientResponseException.class, ex -> {
                    assertThat(ex.getStatusCode().value()).isEqualTo(401);
                    assertThat(errorMessageFromBody(ex)).isEqualTo(expectedMessage);
                });
    }

    private static RestClient client() {
        return IntegrationTestRestClient.create();
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private void assertLoginReturns401InvalidCredentials(LoginRequest loginRequest) {
        RestClient client = IntegrationTestRestClient.create();
        assertThatThrownBy(() -> client.post()
                .uri("http://localhost:" + port + "/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(loginRequest)
                .retrieve()
                .body(TokensResponse.class))
                .isInstanceOfSatisfying(RestClientResponseException.class, ex -> {
                    assertThat(ex.getStatusCode().value()).isEqualTo(401);
                    assertThat(errorMessageFromBody(ex)).isEqualTo("Invalid credentials");
                });
    }

    private static String errorMessageFromBody(RestClientResponseException ex) {
        JsonNode node = JSON.readTree(ex.getResponseBodyAsString());
        JsonNode message = node.get("message");
        return message == null || message.isNull() ? null : message.asString();
    }
}



