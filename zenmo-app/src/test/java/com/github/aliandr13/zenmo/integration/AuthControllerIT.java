package com.github.aliandr13.zenmo.integration;

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
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthControllerIT {

    @LocalServerPort
    private int port;

    @Test
    void registerLoginAndMeFlowWorks() {
        RestClient client = RestClient.create();

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
}



