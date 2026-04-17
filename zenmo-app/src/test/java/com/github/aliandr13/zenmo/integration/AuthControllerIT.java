package com.github.aliandr13.zenmo.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.aliandr13.zenmo.auth.dto.LoginRequest;
import com.github.aliandr13.zenmo.auth.dto.RegisterRequest;
import com.github.aliandr13.zenmo.auth.dto.TokensResponse;
import com.github.aliandr13.zenmo.security.CurrentUser;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthControllerIT {

    private static final ObjectMapper JSON = new ObjectMapper();

    @LocalServerPort
    private int port;

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
        try {
            JsonNode node = JSON.readTree(ex.getResponseBodyAsString());
            JsonNode message = node.get("message");
            return message == null || message.isNull() ? null : message.asText();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}



