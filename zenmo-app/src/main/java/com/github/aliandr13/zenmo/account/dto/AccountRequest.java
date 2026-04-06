package com.github.aliandr13.zenmo.account.dto;

import com.github.aliandr13.zenmo.account.AccountType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Request DTO for creating or updating an account.
 */
public record AccountRequest(
        @NotBlank @Size(max = 200) String name,
        @NotNull AccountType type,
        @NotBlank @Size(min = 3, max = 3) String currency,
        BigDecimal creditLimit,
        BigDecimal currentBalance,
        BigDecimal statementBalance,
        @Min(1) @Max(31) Integer paymentDueDay,
        @NotNull @Min(1) @Max(31) Integer closingDay
) {
}
