package com.github.aliandr13.zenmo.account;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Account domain object (plain POJO).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Account {

    private UUID id;
    private UUID userId;
    private String name;
    private AccountType type;
    private String currency;
    private BigDecimal creditLimit;
    private Integer paymentDueDay;
    private Integer closingDay;
    private boolean archived;
    private Instant createdAt;
}
