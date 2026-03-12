package com.github.aliandr13.zenmo.account;

import com.github.aliandr13.zenmo.account.dto.AccountRequest;
import com.github.aliandr13.zenmo.account.dto.AccountResponse;
import com.github.aliandr13.zenmo.common.NotFoundException;
import com.github.aliandr13.zenmo.security.AuthFacade;
import com.github.aliandr13.zenmo.security.CurrentUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
                .archived(false)
                .createdAt(Instant.now())
                .build();
        given(accountRepository.findByUserIdOrderByCreatedAtDesc(userId)).willReturn(List.of(account));

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

        AccountRequest request = new AccountRequest("Main", AccountType.CHECKING, "USD", null);

        given(accountRepository.save(any(Account.class))).willAnswer(invocation -> invocation.getArgument(0));

        AccountResponse response = accountService.create(request);

        assertThat(response.name()).isEqualTo("Main");
        assertThat(response.type()).isEqualTo(AccountType.CHECKING);
        assertThat(response.currency()).isEqualTo("USD");
        verify(accountRepository).save(any(Account.class));
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

