package com.github.aliandr13.zenmo.account.dto;

import com.github.aliandr13.zenmo.account.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Request DTO for creating or updating an account.
 */
public record AccountRequest(@NotBlank @Size(max = 200) String name, @NotNull AccountType type,
                             @NotBlank @Size(min = 3, max = 3) String currency, BigDecimal creditLimit) {
}
