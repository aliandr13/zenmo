package com.github.aliandr13.zenmo.service;

import com.github.aliandr13.zenmo.account.Account;
import com.github.aliandr13.zenmo.account.AccountType;
import com.github.aliandr13.zenmo.account.dto.AccountRequest;
import com.github.aliandr13.zenmo.account.dto.AccountResponse;
import com.github.aliandr13.zenmo.common.NotFoundException;
import com.github.aliandr13.zenmo.repository.AccountRepository;
import com.github.aliandr13.zenmo.security.AuthFacade;
import com.github.aliandr13.zenmo.security.CurrentUser;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AuthFacade authFacade;

    @InjectMocks
    private AccountService accountService;

    @Test
    void listReturnsAccountsForCurrentUser() {
        UUID userId = UUID.randomUUID();
        given(authFacade.currentUser()).willReturn(new CurrentUser(userId, "user@example.com"));

        Account account = Account.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .name("Main")
                .type(AccountType.CHECKING)
                .currency("USD")
                .creditLimit(null)
                .currentBalance(BigDecimal.ZERO)
                .statementBalance(BigDecimal.ZERO)
                .paymentDueDay(null)
                .closingDay(1)
                .archived(false)
                .createdAt(Instant.now())
                .build();
        given(accountRepository.findByUserIdOrderByCreatedDesc(userId)).willReturn(List.of(account));

        List<AccountResponse> result = accountService.list();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().id()).isEqualTo(account.getId());
    }

    @Test
    void getReturnsAccountForCurrentUser() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        given(authFacade.currentUser()).willReturn(new CurrentUser(userId, "user@example.com"));

        Account account = Account.builder()
                .id(accountId)
                .userId(userId)
                .name("Main")
                .type(AccountType.CHECKING)
                .currency("USD")
                .creditLimit(null)
                .currentBalance(BigDecimal.ZERO)
                .statementBalance(BigDecimal.ZERO)
                .paymentDueDay(null)
                .closingDay(1)
                .archived(false)
                .createdAt(Instant.now())
                .build();
        given(accountRepository.findByIdAndUserId(accountId, userId)).willReturn(Optional.of(account));

        AccountResponse response = accountService.get(accountId);

        assertThat(response.id()).isEqualTo(accountId);
        assertThat(response.name()).isEqualTo("Main");
    }

    @Test
    void getThrowsWhenAccountNotFound() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        given(authFacade.currentUser()).willReturn(new CurrentUser(userId, "user@example.com"));
        given(accountRepository.findByIdAndUserId(accountId, userId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.get(accountId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Account not found");
    }

    @Test
    void createPersistsNewAccountForCurrentUser() {
        UUID userId = UUID.randomUUID();
        given(authFacade.currentUser()).willReturn(new CurrentUser(userId, "user@example.com"));

        AccountRequest request =
                new AccountRequest("Main", AccountType.CHECKING, "USD", null, null, null, null, 1);

        AccountResponse response = accountService.create(request);

        assertThat(response.name()).isEqualTo("Main");
        assertThat(response.type()).isEqualTo(AccountType.CHECKING);
        assertThat(response.currency()).isEqualTo("USD");
        assertThat(response.currentBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.statementBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void createUsesProvidedBalancesWhenSet() {
        UUID userId = UUID.randomUUID();
        given(authFacade.currentUser()).willReturn(new CurrentUser(userId, "user@example.com"));

        AccountRequest request = new AccountRequest(
                "Main",
                AccountType.CHECKING,
                "USD",
                null,
                new BigDecimal("10.00"),
                new BigDecimal("20.50"),
                null,
                1);

        AccountResponse response = accountService.create(request);

        assertThat(response.currentBalance()).isEqualByComparingTo("10.00");
        assertThat(response.statementBalance()).isEqualByComparingTo("20.50");
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void updateMergesRequestAndPersists() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        given(authFacade.currentUser()).willReturn(new CurrentUser(userId, "user@example.com"));

        Account existing = Account.builder()
                .id(accountId)
                .userId(userId)
                .name("Old")
                .type(AccountType.CHECKING)
                .currency("USD")
                .creditLimit(null)
                .currentBalance(new BigDecimal("50.00"))
                .statementBalance(new BigDecimal("40.00"))
                .paymentDueDay(null)
                .closingDay(1)
                .archived(false)
                .createdAt(Instant.parse("2024-01-01T00:00:00Z"))
                .build();
        given(accountRepository.findByIdAndUserId(accountId, userId)).willReturn(Optional.of(existing));

        AccountRequest request =
                new AccountRequest("New", AccountType.SAVINGS, "EUR", null, null, null, null, 1);

        AccountResponse response = accountService.update(accountId, request);

        assertThat(response.name()).isEqualTo("New");
        assertThat(response.type()).isEqualTo(AccountType.SAVINGS);
        assertThat(response.currency()).isEqualTo("EUR");
        assertThat(response.currentBalance()).isEqualByComparingTo("50.00");
        assertThat(response.statementBalance()).isEqualByComparingTo("40.00");
        assertThat(response.archived()).isFalse();

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).update(captor.capture());
        Account updated = captor.getValue();
        assertThat(updated.getId()).isEqualTo(accountId);
        assertThat(updated.getUserId()).isEqualTo(userId);
        assertThat(updated.getCreatedAt()).isEqualTo(existing.getCreatedAt());
        assertThat(updated.getCurrentBalance()).isEqualByComparingTo("50.00");
        assertThat(updated.getStatementBalance()).isEqualByComparingTo("40.00");
    }

    @Test
    void updateAppliesBalancesWhenProvided() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        given(authFacade.currentUser()).willReturn(new CurrentUser(userId, "user@example.com"));

        Account existing = Account.builder()
                .id(accountId)
                .userId(userId)
                .name("Main")
                .type(AccountType.CHECKING)
                .currency("USD")
                .creditLimit(null)
                .currentBalance(BigDecimal.ZERO)
                .statementBalance(BigDecimal.ZERO)
                .paymentDueDay(null)
                .closingDay(1)
                .archived(false)
                .createdAt(Instant.now())
                .build();
        given(accountRepository.findByIdAndUserId(accountId, userId)).willReturn(Optional.of(existing));

        AccountRequest request = new AccountRequest(
                "Main",
                AccountType.CHECKING,
                "USD",
                null,
                new BigDecimal("12.34"),
                new BigDecimal("56.78"),
                null,
                1);

        accountService.update(accountId, request);

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).update(captor.capture());
        assertThat(captor.getValue().getCurrentBalance()).isEqualByComparingTo("12.34");
        assertThat(captor.getValue().getStatementBalance()).isEqualByComparingTo("56.78");
    }

    @Test
    void updateThrowsWhenAccountNotFound() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        given(authFacade.currentUser()).willReturn(new CurrentUser(userId, "user@example.com"));
        given(accountRepository.findByIdAndUserId(accountId, userId)).willReturn(Optional.empty());

        AccountRequest request =
                new AccountRequest("X", AccountType.CASH, "USD", null, null, null, null, 1);

        assertThatThrownBy(() -> accountService.update(accountId, request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Account not found");

        verifyNoMoreInteractions(accountRepository);
    }

    @Test
    void deleteDeletesWhenAccountExistsForUser() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        given(authFacade.currentUser()).willReturn(new CurrentUser(userId, "user@example.com"));
        given(accountRepository.existsByIdAndUserId(accountId, userId)).willReturn(true);

        accountService.delete(accountId);

        verify(accountRepository).deleteById(eq(accountId));
    }

    @Test
    void deleteThrowsWhenAccountNotFoundForUser() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        given(authFacade.currentUser()).willReturn(new CurrentUser(userId, "user@example.com"));
        given(accountRepository.existsByIdAndUserId(accountId, userId)).willReturn(false);

        assertThatThrownBy(() -> accountService.delete(accountId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Account not found");

        verifyNoMoreInteractions(accountRepository);
    }
}

