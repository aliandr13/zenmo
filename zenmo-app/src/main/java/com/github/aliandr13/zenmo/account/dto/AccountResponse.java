package com.github.aliandr13.zenmo.account.dto;

import com.github.aliandr13.zenmo.account.Account;
import com.github.aliandr13.zenmo.account.AccountType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for an account.
 */
public record AccountResponse(UUID id,
                              String name,
                              AccountType type,
                              String currency,
                              BigDecimal creditLimit,
                              BigDecimal currentBalance,
                              BigDecimal statementBalance,
                              Integer paymentDueDay,
                              Integer closingDay,
                              boolean archived,
                              Instant createdAt) {
    /**
     * Builds a response from an entity.
     */
    public static AccountResponse from(Account a) {
        return new AccountResponse(a.getId(), a.getName(), a.getType(),
                a.getCurrency(), a.getCreditLimit(),
                a.getCurrentBalance(), a.getStatementBalance(),
                a.getPaymentDueDay(), a.getClosingDay(),
                a.isArchived(), a.getCreatedAt());
    }
}
