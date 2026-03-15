package com.github.aliandr13.zenmo.transaction;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * JPA repository for transactions.
 */
public interface TxnRepository extends JpaRepository<Txn, UUID> {

    /**
     * Returns a page of transactions for a user, newest first.
     */
    Page<Txn> findByUserIdOrderByTransactionDateDescCreatedAtDesc(UUID userId, Pageable pageable);

    /**
     * Returns a page of transactions for a user and account, newest first.
     */
    Page<Txn> findByUserIdAndAccountIdOrderByTransactionDateDescCreatedAtDesc(
            UUID userId, UUID accountId, Pageable pageable);

    /**
     * Returns a page of transactions for a user in a date range, newest first.
     */
    Page<Txn> findByUserIdAndTransactionDateBetweenOrderByTransactionDateDescCreatedAtDesc(
            UUID userId, LocalDate from, LocalDate to, Pageable pageable);

    /**
     * Finds a transaction by id and owner user.
     */
    Optional<Txn> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Returns whether a transaction exists for the given id and user.
     */
    boolean existsByIdAndUserId(UUID id, UUID userId);
}
