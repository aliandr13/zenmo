package com.github.aliandr13.zenmo.transaction.dto;

import com.github.aliandr13.zenmo.transaction.TransactionStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Request DTO for creating or updating a transaction.
 */
public record TxnRequest(
        @NotNull UUID accountId,
        UUID categoryId,
        @NotNull LocalDate transactionDate,
        LocalDate postDate,
        @NotNull BigDecimal amount,
        @NotBlank @Size(min = 3, max = 3) String currency,
        @NotBlank @Size(max = 500) String description,
        @Size(max = 200) String merchant,
        @NotNull TransactionStatus status,
        @Size(max = 1000) String notes
) {
}
