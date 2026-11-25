package com.github.aliandr13.zenmo.service;

import com.github.aliandr13.zenmo.account.Account;
import com.github.aliandr13.zenmo.account.AccountType;
import com.github.aliandr13.zenmo.common.NotFoundException;
import com.github.aliandr13.zenmo.repository.AccountRepository;
import com.github.aliandr13.zenmo.security.AuthFacade;
import com.github.aliandr13.zenmo.security.CurrentUser;
import com.github.aliandr13.zenmo.transaction.TransactionStatus;
import com.github.aliandr13.zenmo.transaction.Txn;
import com.github.aliandr13.zenmo.transaction.TxnRepository;
import com.github.aliandr13.zenmo.transaction.TxnService;
import com.github.aliandr13.zenmo.transaction.dto.TxnRequest;
import com.github.aliandr13.zenmo.transaction.dto.TxnResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TxnServiceTest {

    @Mock
    private TxnRepository txnRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AuthFacade authFacade;

    @InjectMocks
    private TxnService txnService;

    @Test
    void listFiltersByAccountIdWhenPresent() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        given(authFacade.currentUser()).willReturn(new CurrentUser(userId, "user@example.com"));

        Txn txn = new Txn(
                UUID.randomUUID(),
                userId,
                accountId,
                null,
                LocalDate.now(),
                LocalDate.now(),
                BigDecimal.valueOf(10000L),
                "USD",
                "desc",
                "merchant",
                TransactionStatus.PENDING,
                null,
                Instant.now()
        );
        given(accountRepository.existsByIdAndUserId(accountId, userId)).willReturn(true);
        given(txnRepository.findByUserIdAndAccountIdOrderByTransactionDateDescCreatedAtDesc(userId, accountId, pageable))
                .willReturn(new PageImpl<>(List.of(txn)));

        Page<TxnResponse> page = txnService.list(Optional.of(accountId), Optional.empty(), Optional.empty(), pageable);

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().getFirst().accountId()).isEqualTo(accountId);
    }

    @Test
    void listThrowsWhenAccountDoesNotBelongToUser() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        given(authFacade.currentUser()).willReturn(new CurrentUser(userId, "user@example.com"));
        given(accountRepository.existsByIdAndUserId(accountId, userId)).willReturn(false);

        assertThatThrownBy(() -> txnService.list(Optional.of(accountId), Optional.empty(), Optional.empty(), pageable))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Account not found");
    }

    @Test
    void getReturnsTxnForCurrentUser() {
        UUID userId = UUID.randomUUID();
        UUID txnId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        given(authFacade.currentUser()).willReturn(new CurrentUser(userId, "user@example.com"));

        Txn txn = new Txn(
                txnId,
                userId,
                accountId,
                null,
                LocalDate.now(),
                LocalDate.now(),
                BigDecimal.valueOf(10000L),
                "USD",
                "desc",
                "merchant",
                TransactionStatus.PENDING,
                null,
                Instant.now()
        );
        given(txnRepository.findByIdAndUserId(txnId, userId)).willReturn(Optional.of(txn));

        TxnResponse response = txnService.get(txnId);

        assertThat(response.id()).isEqualTo(txnId);
        assertThat(response.accountId()).isEqualTo(accountId);
    }

    @Test
    void getThrowsWhenTxnNotFound() {
        UUID userId = UUID.randomUUID();
        UUID txnId = UUID.randomUUID();
        given(authFacade.currentUser()).willReturn(new CurrentUser(userId, "user@example.com"));
        given(txnRepository.findByIdAndUserId(txnId, userId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> txnService.get(txnId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Transaction not found");
    }

    @Test
    void createPendingPersistsTxnWithoutChangingBalance() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        givenUser(userId);
        givenAccount(accountId, userId, AccountType.CHECKING, "USD");

        TxnResponse response = txnService.create(
                request(accountId, "10000", "USD", TransactionStatus.PENDING));

        assertThat(response.accountId()).isEqualTo(accountId);
        assertThat(response.amount()).isEqualByComparingTo("10000");
        verify(txnRepository).save(any(Txn.class));
        verify(accountRepository, never()).adjustBalance(any(), any(), any());
    }

    @ParameterizedTest
    @CsvSource({
        "CHECKING, -50.00",
        "CHECKING, 100.00",
        "CASH, -12.34",
        "SAVINGS, 250.00",
        "CREDIT, -50.00",
        "CREDIT, 75.00"
    })
    void createPostedAdjustsBalanceBySignedAmount(AccountType type, BigDecimal amount) {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        givenUser(userId);
        givenAccount(accountId, userId, type, "USD");

        txnService.create(request(accountId, amount.toPlainString(), "USD", TransactionStatus.POSTED));

        InOrder inOrder = inOrder(txnRepository, accountRepository);
        inOrder.verify(txnRepository).save(any(Txn.class));
        inOrder.verify(accountRepository).adjustBalance(accountId, userId, amount);
    }

    @Test
    void createAcceptsCurrencyInDifferentCase() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        givenUser(userId);
        givenAccount(accountId, userId, AccountType.CHECKING, "USD");

        txnService.create(request(accountId, "-5.00", "usd", TransactionStatus.POSTED));

        verify(accountRepository).adjustBalance(accountId, userId, new BigDecimal("-5.00"));
    }

    @Test
    void createThrowsWhenCurrencyDoesNotMatchAccount() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        givenUser(userId);
        givenAccount(accountId, userId, AccountType.CHECKING, "USD");

        assertThatThrownBy(() -> txnService.create(
                request(accountId, "-5.00", "EUR", TransactionStatus.POSTED)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("EUR")
                .hasMessageContaining("USD");
        verify(txnRepository, never()).save(any());
        verify(accountRepository, never()).adjustBalance(any(), any(), any());
    }

    @Test
    void createThrowsWhenAccountNotFoundForUser() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        givenUser(userId);
        given(accountRepository.findByIdAndUserId(accountId, userId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> txnService.create(
                request(accountId, "10000", "USD", TransactionStatus.POSTED)))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Account not found");
        verify(txnRepository, never()).save(any());
        verify(accountRepository, never()).adjustBalance(any(), any(), any());
    }

    @Test
    void deletePostedReversesBalance() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID txnId = UUID.randomUUID();
        givenUser(userId);
        given(txnRepository.findByIdAndUserId(txnId, userId)).willReturn(Optional.of(
                txn(txnId, userId, accountId, "-50.00", TransactionStatus.POSTED)));

        txnService.delete(txnId);

        verify(txnRepository).deleteById(eq(txnId));
        verify(accountRepository).adjustBalance(accountId, userId, new BigDecimal("50.00"));
    }

    @Test
    void deletePendingDoesNotChangeBalance() {
        UUID userId = UUID.randomUUID();
        UUID txnId = UUID.randomUUID();
        givenUser(userId);
        given(txnRepository.findByIdAndUserId(txnId, userId)).willReturn(Optional.of(
                txn(txnId, userId, UUID.randomUUID(), "-50.00", TransactionStatus.PENDING)));

        txnService.delete(txnId);

        verify(txnRepository).deleteById(eq(txnId));
        verify(accountRepository, never()).adjustBalance(any(), any(), any());
    }

    @Test
    void deleteThrowsWhenTxnNotFoundForUser() {
        UUID userId = UUID.randomUUID();
        UUID txnId = UUID.randomUUID();
        givenUser(userId);
        given(txnRepository.findByIdAndUserId(txnId, userId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> txnService.delete(txnId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Transaction not found");
        verify(txnRepository, never()).deleteById(any());
        verify(accountRepository, never()).adjustBalance(any(), any(), any());
    }

    private void givenUser(UUID userId) {
        given(authFacade.currentUser()).willReturn(new CurrentUser(userId, "user@example.com"));
    }

    private void givenAccount(UUID accountId, UUID userId, AccountType type, String currency) {
        Account account = Account.builder()
                .id(accountId)
                .userId(userId)
                .name("Acc")
                .type(type)
                .currency(currency)
                .currentBalance(BigDecimal.ZERO)
                .statementBalance(BigDecimal.ZERO)
                .createdAt(Instant.now())
                .build();
        given(accountRepository.findByIdAndUserId(accountId, userId)).willReturn(Optional.of(account));
    }

    private static TxnRequest request(UUID accountId, String amount, String currency, TransactionStatus status) {
        return new TxnRequest(
                accountId,
                null,
                LocalDate.now(),
                LocalDate.now(),
                new BigDecimal(amount),
                currency,
                "desc",
                "merchant",
                status,
                null
        );
    }

    private static Txn txn(UUID id, UUID userId, UUID accountId, String amount, TransactionStatus status) {
        return new Txn(
                id,
                userId,
                accountId,
                null,
                LocalDate.now(),
                null,
                new BigDecimal(amount),
                "USD",
                "desc",
                null,
                status,
                null,
                Instant.now()
        );
    }
}
