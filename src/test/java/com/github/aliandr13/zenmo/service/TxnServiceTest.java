package com.github.aliandr13.zenmo.service;

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
    void createPersistsTxnWhenAccountBelongsToUser() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        given(authFacade.currentUser()).willReturn(new CurrentUser(userId, "user@example.com"));
        given(accountRepository.existsByIdAndUserId(accountId, userId)).willReturn(true);

        TxnRequest request = new TxnRequest(
                accountId,
                null,
                LocalDate.now(),
                LocalDate.now(),
                BigDecimal.valueOf(10000L),
                "USD",
                "desc",
                "merchant",
                TransactionStatus.PENDING,
                null
        );

        TxnResponse response = txnService.create(request);

        assertThat(response.accountId()).isEqualTo(accountId);
        assertThat(response.amount()).isEqualTo(BigDecimal.valueOf(10000L));
        verify(txnRepository).save(any(Txn.class));
    }

    @Test
    void createThrowsWhenAccountNotFoundForUser() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        given(authFacade.currentUser()).willReturn(new CurrentUser(userId, "user@example.com"));
        given(accountRepository.existsByIdAndUserId(accountId, userId)).willReturn(false);

        TxnRequest request = new TxnRequest(
                accountId,
                null,
                LocalDate.now(),
                LocalDate.now(),
                BigDecimal.valueOf(10000L),
                "USD",
                "desc",
                "merchant",
                TransactionStatus.PENDING,
                null
        );

        assertThatThrownBy(() -> txnService.create(request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Account not found");
    }

    @Test
    void deleteDeletesWhenTxnExistsForUser() {
        UUID userId = UUID.randomUUID();
        UUID txnId = UUID.randomUUID();
        given(authFacade.currentUser()).willReturn(new CurrentUser(userId, "user@example.com"));
        given(txnRepository.existsByIdAndUserId(txnId, userId)).willReturn(true);

        txnService.delete(txnId);

        verify(txnRepository).deleteById(eq(txnId));
    }

    @Test
    void deleteThrowsWhenTxnNotFoundForUser() {
        UUID userId = UUID.randomUUID();
        UUID txnId = UUID.randomUUID();
        given(authFacade.currentUser()).willReturn(new CurrentUser(userId, "user@example.com"));
        given(txnRepository.existsByIdAndUserId(txnId, userId)).willReturn(false);

        assertThatThrownBy(() -> txnService.delete(txnId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Transaction not found");
    }
}

