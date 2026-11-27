package com.github.aliandr13.zenmo.transaction;

import com.github.aliandr13.zenmo.account.Account;
import com.github.aliandr13.zenmo.common.NotFoundException;
import com.github.aliandr13.zenmo.repository.AccountRepository;
import com.github.aliandr13.zenmo.security.AuthFacade;
import com.github.aliandr13.zenmo.security.CurrentUser;
import com.github.aliandr13.zenmo.transaction.dto.TxnRequest;
import com.github.aliandr13.zenmo.transaction.dto.TxnResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service for transaction operations.
 */
@Service
public class TxnService {

    private final TxnRepository txnRepository;
    private final AccountRepository accountRepository;
    private final AuthFacade authFacade;

    /**
     * Constructor.
     */
    public TxnService(
            TxnRepository txnRepository,
            AccountRepository accountRepository,
            AuthFacade authFacade) {
        this.txnRepository = txnRepository;
        this.accountRepository = accountRepository;
        this.authFacade = authFacade;
    }

    /**
     * Returns a page of transactions for the current user, optionally filtered.
     */
    @Transactional(readOnly = true)
    public Page<TxnResponse> list(
            Optional<UUID> accountId,
            Optional<LocalDate> fromDate,
            Optional<LocalDate> toDate,
            Pageable pageable) {
        CurrentUser user = authFacade.currentUser();
        if (accountId.isPresent()) {
            if (!accountRepository.existsByIdAndUserId(accountId.get(), user.userId())) {
                throw new NotFoundException("Account not found");
            }
            return txnRepository
                    .findByUserIdAndAccountIdOrderByTransactionDateDescCreatedAtDesc(
                            user.userId(), accountId.get(), pageable)
                    .map(TxnResponse::from);
        }
        if (fromDate.isPresent() && toDate.isPresent()) {
            return txnRepository
                    .findByUserIdAndTransactionDateBetweenOrderByTransactionDateDescCreatedAtDesc(
                            user.userId(), fromDate.get(), toDate.get(), pageable)
                    .map(TxnResponse::from);
        }
        return txnRepository
                .findByUserIdOrderByTransactionDateDescCreatedAtDesc(user.userId(), pageable)
                .map(TxnResponse::from);
    }

    /**
     * Returns a single transaction by id for the current user.
     */
    @Transactional(readOnly = true)
    public TxnResponse get(UUID id) {
        CurrentUser user = authFacade.currentUser();
        Txn txn = txnRepository
                .findByIdAndUserId(id, user.userId())
                .orElseThrow(() -> new NotFoundException("Transaction not found"));
        return TxnResponse.from(txn);
    }

    /**
     * Creates a new transaction for the current user.
     *
     * <p>A {@link TransactionStatus#POSTED} transaction adds its signed amount to the account's current
     * balance (negative = money out, positive = money in). A pending transaction leaves the balance as is.
     */
    @Transactional
    public TxnResponse create(TxnRequest request) {
        CurrentUser user = authFacade.currentUser();
        Account account = accountRepository
                .findByIdAndUserId(request.accountId(), user.userId())
                .orElseThrow(() -> new NotFoundException("Account not found"));
        if (!account.getCurrency().equalsIgnoreCase(request.currency())) {
            throw new IllegalArgumentException(
                    "Transaction currency " + request.currency()
                            + " does not match account currency " + account.getCurrency());
        }
        Txn txn = new Txn(
                UUID.randomUUID(),
                user.userId(),
                request.accountId(),
                request.categoryId(),
                request.transactionDate(),
                request.postDate(),
                request.amount(),
                request.currency(),
                request.description(),
                request.merchant(),
                request.status(),
                Instant.now()
        );
        txnRepository.save(txn);
        if (txn.getStatus() == TransactionStatus.POSTED) {
            accountRepository.adjustBalance(txn.getAccountId(), user.userId(), txn.getAmount());
        }
        return TxnResponse.from(txn);
    }

    /**
     * Deletes a transaction by id for the current user.
     *
     * <p>Deleting a {@link TransactionStatus#POSTED} transaction reverses its effect on the account balance.
     */
    @Transactional
    public void delete(UUID id) {
        CurrentUser user = authFacade.currentUser();
        Txn txn = txnRepository
                .findByIdAndUserId(id, user.userId())
                .orElseThrow(() -> new NotFoundException("Transaction not found"));
        txnRepository.deleteById(id);
        if (txn.getStatus() == TransactionStatus.POSTED) {
            accountRepository.adjustBalance(txn.getAccountId(), user.userId(), txn.getAmount().negate());
        }
    }
}
