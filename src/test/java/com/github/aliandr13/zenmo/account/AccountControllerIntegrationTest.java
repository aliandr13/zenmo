package com.github.aliandr13.zenmo.account;

import com.github.aliandr13.zenmo.auth.dto.LoginRequest;
import com.github.aliandr13.zenmo.auth.dto.RegisterRequest;
import com.github.aliandr13.zenmo.auth.dto.TokensResponse;
import com.github.aliandr13.zenmo.account.dto.AccountRequest;
import com.github.aliandr13.zenmo.account.dto.AccountResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AccountControllerIntegrationTest {

    @LocalServerPort
    private int port;

    private String base() {
        return "http://localhost:" + port;
    }

    private String loginAndGetToken() {
        RestClient client = RestClient.create();
        client.post()
                .uri(base() + "/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new RegisterRequest("mvp@example.com", "password123"))
                .retrieve()
                .toBodilessEntity();
        TokensResponse tokens = client.post()
                .uri(base() + "/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoginRequest("mvp@example.com", "password123"))
                .retrieve()
                .body(TokensResponse.class);
        return tokens.accessToken();
    }

    @Test
    void createAndListAccounts() {
        String token = loginAndGetToken();
        RestClient client = RestClient.create();

        AccountRequest create = new AccountRequest("Main Checking", com.github.aliandr13.zenmo.account.AccountType.CHECKING, "USD", null);
        AccountResponse created = client.post()
                .uri(base() + "/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .body(create)
                .retrieve()
                .body(AccountResponse.class);

        assertThat(created).isNotNull();
        assertThat(created.id()).isNotNull();
        assertThat(created.name()).isEqualTo("Main Checking");
        assertThat(created.type()).isEqualTo(com.github.aliandr13.zenmo.account.AccountType.CHECKING);
        assertThat(created.currency()).isEqualTo("USD");
        assertThat(created.archived()).isFalse();

        List<AccountResponse> list = client.get()
                .uri(base() + "/api/accounts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .body(new ParameterizedTypeReference<List<AccountResponse>>() {});

        assertThat(list).hasSize(1);
        assertThat(list.get(0).id()).isEqualTo(created.id());
    }
}
