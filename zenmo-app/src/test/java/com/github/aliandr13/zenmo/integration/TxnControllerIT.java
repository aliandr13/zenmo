package com.github.aliandr13.zenmo.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.aliandr13.zenmo.account.dto.AccountRequest;
import com.github.aliandr13.zenmo.account.dto.AccountResponse;
import com.github.aliandr13.zenmo.auth.dto.LoginRequest;
import com.github.aliandr13.zenmo.auth.dto.RegisterRequest;
import com.github.aliandr13.zenmo.auth.dto.TokensResponse;
import com.github.aliandr13.zenmo.transaction.TransactionStatus;
import com.github.aliandr13.zenmo.transaction.dto.TxnRequest;
import com.github.aliandr13.zenmo.transaction.dto.TxnResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
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
class TxnControllerIT {

    @LocalServerPort
    private int port;

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PageResponse(List<TxnResponse> content, long totalElements) {
    }

    private String base() {
        return "http://localhost:" + port;
    }

    private String loginAndGetToken() {
        RestClient client = IntegrationTestRestClient.create();
        client.post()
                .uri(base() + "/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new RegisterRequest("txn@example.com", "password123"))
                .retrieve()
                .toBodilessEntity();
        TokensResponse tokens = client.post()
                .uri(base() + "/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoginRequest("txn@example.com", "password123"))
                .retrieve()
                .body(TokensResponse.class);
        return tokens.accessToken();
    }

    @Test
    void createListGetAndDeleteTransaction() {
        String token = loginAndGetToken();
        RestClient client = IntegrationTestRestClient.create();

        AccountRequest accountRequest =
                new AccountRequest("Main Checking", CHECKING, "USD", null, null, null, null, null);
        AccountResponse account = client.post()
                .uri(base() + "/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .body(accountRequest)
                .retrieve()
                .body(AccountResponse.class);
        assertThat(account).isNotNull();
        assertThat(account.id()).isNotNull();

        LocalDate txnDate = LocalDate.now();
        TxnRequest txnRequest = new TxnRequest(
                account.id(),
                null,
                txnDate,
                null,
                new BigDecimal("-50.00"),
                "USD",
                "Coffee shop",
                null,
                TransactionStatus.POSTED,
                null
        );
        TxnResponse created = client.post()
                .uri(base() + "/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .body(txnRequest)
                .retrieve()
                .body(TxnResponse.class);

        assertThat(created).isNotNull();
        assertThat(created.id()).isNotNull();
        assertThat(created.accountId()).isEqualTo(account.id());
        assertThat(created.amount()).isEqualByComparingTo(new BigDecimal("-50.00"));
        assertThat(created.description()).isEqualTo("Coffee shop");
        assertThat(created.status()).isEqualTo(TransactionStatus.POSTED);
        assertThat(created.currency()).isEqualTo("USD");
        assertThat(created.transactionDate()).isEqualTo(txnDate);

        PageResponse page = client.get()
                .uri(base() + "/api/transactions?size=10")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .body(new ParameterizedTypeReference<PageResponse>() {
                });

        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.content()).hasSize(1);
        assertThat(page.content().getFirst().id()).isEqualTo(created.id());

        TxnResponse gotten = client.get()
                .uri(base() + "/api/transactions/" + created.id())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .body(TxnResponse.class);
        assertThat(gotten.id()).isEqualTo(created.id());
        assertThat(gotten.description()).isEqualTo("Coffee shop");

        client.delete()
                .uri(base() + "/api/transactions/" + created.id())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .toBodilessEntity();

        PageResponse pageAfterDelete = client.get()
                .uri(base() + "/api/transactions?size=10")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .body(new ParameterizedTypeReference<PageResponse>() {
                });
        assertThat(pageAfterDelete.totalElements()).isZero();
        assertThat(pageAfterDelete.content()).isEmpty();
    }
}
