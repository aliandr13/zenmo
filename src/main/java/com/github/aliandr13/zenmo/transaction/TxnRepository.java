package com.github.aliandr13.zenmo.transaction;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface TxnRepository extends JpaRepository<Txn, UUID> {

    Page<Txn> findByUserIdOrderByTransactionDateDescCreatedAtDesc(UUID userId, Pageable pageable);

    Page<Txn> findByUserIdAndAccountIdOrderByTransactionDateDescCreatedAtDesc(UUID userId, UUID accountId, Pageable pageable);

    Page<Txn> findByUserIdAndTransactionDateBetweenOrderByTransactionDateDescCreatedAtDesc(
            UUID userId, LocalDate from, LocalDate to, Pageable pageable);

    Optional<Txn> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByIdAndUserId(UUID id, UUID userId);
}
