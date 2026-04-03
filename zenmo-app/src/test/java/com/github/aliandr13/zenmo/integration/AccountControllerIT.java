package com.github.aliandr13.zenmo.integration;

import com.github.aliandr13.zenmo.account.dto.AccountRequest;
import com.github.aliandr13.zenmo.account.dto.AccountResponse;
import com.github.aliandr13.zenmo.auth.dto.LoginRequest;
import com.github.aliandr13.zenmo.auth.dto.RegisterRequest;
import com.github.aliandr13.zenmo.auth.dto.TokensResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import static com.github.aliandr13.zenmo.account.AccountType.CHECKING;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AccountControllerIT {

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

        // CREATE account
        AccountRequest create = new AccountRequest("Main Checking", CHECKING, "USD", null, null, 1);
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
        assertThat(created.type()).isEqualTo(CHECKING);
        assertThat(created.currency()).isEqualTo("USD");
        assertThat(created.archived()).isFalse();

        // GET all accounts
        List<AccountResponse> list = client.get()
                .uri(base() + "/api/accounts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .body(new ParameterizedTypeReference<List<AccountResponse>>() {
                });

        assertThat(list).hasSize(1);
        assertThat(list.getFirst().id()).isEqualTo(created.id());

        // DELETE ACCOUNT
        client.delete()
                .uri(base() + "/api/accounts/" + created.id())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .toBodilessEntity();

        // GET all accounts
        list = client.get()
                .uri(base() + "/api/accounts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        assertThat(list).isEmpty();
    }
}
