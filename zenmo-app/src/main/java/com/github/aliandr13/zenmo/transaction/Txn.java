package com.github.aliandr13.zenmo.transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Transaction domain object (plain POJO).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Txn {

    private UUID id;
    private UUID userId;
    private UUID accountId;
    private UUID categoryId;
    private LocalDate transactionDate;
    private LocalDate postDate;
    private BigDecimal amount;
    private String currency;
    private String description;
    private String merchant;
    private TransactionStatus status;
    private String notes;
    private Instant createdAt;
}
