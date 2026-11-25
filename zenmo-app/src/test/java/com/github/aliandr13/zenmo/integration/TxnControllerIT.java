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
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import static com.github.aliandr13.zenmo.account.AccountType.CHECKING;
import static com.github.aliandr13.zenmo.account.AccountType.CREDIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        String email = "txn-" + UUID.randomUUID() + "@example.com";
        RestClient client = IntegrationTestRestClient.create();
        client.post()
                .uri(base() + "/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new RegisterRequest(email, "password123"))
                .retrieve()
                .toBodilessEntity();
        TokensResponse tokens = client.post()
                .uri(base() + "/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new LoginRequest(email, "password123"))
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

    @Test
    void postedTransactionChangesBalanceAndDeleteRestoresIt() {
        String token = loginAndGetToken();
        AccountResponse account = createAccount(token,
                new AccountRequest("Checking", CHECKING, "USD", null, new BigDecimal("1000.00"), null, null, null));

        TxnResponse purchase = createTxn(token, account.id(), "-50.00", "USD", TransactionStatus.POSTED);
        assertThat(getAccount(token, account.id()).currentBalance()).isEqualByComparingTo("950.00");

        TxnResponse income = createTxn(token, account.id(), "200.00", "USD", TransactionStatus.POSTED);
        assertThat(getAccount(token, account.id()).currentBalance()).isEqualByComparingTo("1150.00");

        deleteTxn(token, purchase.id());
        assertThat(getAccount(token, account.id()).currentBalance()).isEqualByComparingTo("1200.00");

        deleteTxn(token, income.id());
        assertThat(getAccount(token, account.id()).currentBalance()).isEqualByComparingTo("1000.00");
    }

    @Test
    void pendingTransactionDoesNotChangeBalance() {
        String token = loginAndGetToken();
        AccountResponse account = createAccount(token,
                new AccountRequest("Checking", CHECKING, "USD", null, new BigDecimal("1000.00"), null, null, null));

        TxnResponse pending = createTxn(token, account.id(), "-50.00", "USD", TransactionStatus.PENDING);
        assertThat(getAccount(token, account.id()).currentBalance()).isEqualByComparingTo("1000.00");

        deleteTxn(token, pending.id());
        assertThat(getAccount(token, account.id()).currentBalance()).isEqualByComparingTo("1000.00");
    }

    @Test
    void creditCardPurchaseMakesBalanceMoreNegative() {
        String token = loginAndGetToken();
        AccountResponse card = createAccount(token, new AccountRequest(
                "Visa", CREDIT, "USD", new BigDecimal("5000.00"), new BigDecimal("-200.00"), null, 10, 5));

        createTxn(token, card.id(), "-50.00", "USD", TransactionStatus.POSTED);
        assertThat(getAccount(token, card.id()).currentBalance()).isEqualByComparingTo("-250.00");

        createTxn(token, card.id(), "100.00", "USD", TransactionStatus.POSTED);
        assertThat(getAccount(token, card.id()).currentBalance()).isEqualByComparingTo("-150.00");
    }

    @Test
    void currencyMismatchIsRejectedAndBalanceUnchanged() {
        String token = loginAndGetToken();
        AccountResponse account = createAccount(token,
                new AccountRequest("Checking", CHECKING, "USD", null, new BigDecimal("1000.00"), null, null, null));

        assertThatThrownBy(() -> createTxn(token, account.id(), "-50.00", "EUR", TransactionStatus.POSTED))
                .isInstanceOf(HttpClientErrorException.BadRequest.class);

        assertThat(getAccount(token, account.id()).currentBalance()).isEqualByComparingTo("1000.00");
        PageResponse page = IntegrationTestRestClient.create().get()
                .uri(base() + "/api/transactions?size=10")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .body(new ParameterizedTypeReference<PageResponse>() {
                });
        assertThat(page.totalElements()).isZero();
    }

    private AccountResponse createAccount(String token, AccountRequest request) {
        return IntegrationTestRestClient.create().post()
                .uri(base() + "/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .body(request)
                .retrieve()
                .body(AccountResponse.class);
    }

    private AccountResponse getAccount(String token, UUID id) {
        return IntegrationTestRestClient.create().get()
                .uri(base() + "/api/accounts/" + id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .body(AccountResponse.class);
    }

    private TxnResponse createTxn(
            String token, UUID accountId, String amount, String currency, TransactionStatus status) {
        TxnRequest request = new TxnRequest(
                accountId, null, LocalDate.now(), null, new BigDecimal(amount), currency,
                "Test txn", null, status, null);
        return IntegrationTestRestClient.create().post()
                .uri(base() + "/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .body(request)
                .retrieve()
                .body(TxnResponse.class);
    }

    private void deleteTxn(String token, UUID id) {
        IntegrationTestRestClient.create().delete()
                .uri(base() + "/api/transactions/" + id)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .toBodilessEntity();
    }
}
