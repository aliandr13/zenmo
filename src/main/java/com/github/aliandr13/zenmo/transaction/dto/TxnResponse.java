package com.github.aliandr13.zenmo.transaction.dto;

import com.github.aliandr13.zenmo.transaction.Txn;
import com.github.aliandr13.zenmo.transaction.TransactionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TxnResponse(
        UUID id,
        UUID accountId,
        UUID categoryId,
        LocalDate transactionDate,
        LocalDate postDate,
        BigDecimal amount,
        String currency,
        String description,
        String merchant,
        TransactionStatus status,
        String notes,
        Instant createdAt
) {
    public static TxnResponse from(Txn t) {
        return new TxnResponse(
                t.getId(),
                t.getAccountId(),
                t.getCategoryId(),
                t.getTransactionDate(),
                t.getPostDate(),
                t.getAmount(),
                t.getCurrency(),
                t.getDescription(),
                t.getMerchant(),
                t.getStatus(),
                t.getNotes(),
                t.getCreatedAt()
        );
    }
}
