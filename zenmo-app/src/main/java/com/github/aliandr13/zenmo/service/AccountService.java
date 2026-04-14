package com.github.aliandr13.zenmo.service;

import com.github.aliandr13.zenmo.account.Account;
import com.github.aliandr13.zenmo.account.AccountType;
import com.github.aliandr13.zenmo.account.dto.AccountRequest;
import com.github.aliandr13.zenmo.account.dto.AccountResponse;
import com.github.aliandr13.zenmo.common.NotFoundException;
import com.github.aliandr13.zenmo.repository.AccountRepository;
import com.github.aliandr13.zenmo.security.AuthFacade;
import com.github.aliandr13.zenmo.security.CurrentUser;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service for account operations.
 */
@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final AuthFacade authFacade;

    /**
     * Constructor.
     */
    public AccountService(AccountRepository accountRepository, AuthFacade authFacade) {
        this.accountRepository = accountRepository;
        this.authFacade = authFacade;
    }

    /**
     * Returns all accounts for the current user.
     */
    @Transactional(readOnly = true)
    public List<AccountResponse> list() {
        CurrentUser user = authFacade.currentUser();
        return accountRepository.findByUserIdOrderByCreatedDesc(user.userId())
                .stream()
                .map(AccountResponse::from)
                .toList();
    }

    /**
     * Returns a single account by id for the current user.
     */
    @Transactional(readOnly = true)
    public AccountResponse get(UUID id) {
        CurrentUser user = authFacade.currentUser();
        Account account = accountRepository.findByIdAndUserId(id, user.userId())
                .orElseThrow(() -> new NotFoundException("Account not found"));
        return AccountResponse.from(account);
    }

    /**
     * Creates a new account for the current user.
     */
    @Transactional
    public AccountResponse create(AccountRequest request) {
        CurrentUser user = authFacade.currentUser();
        CreditSchedule schedule = resolveCreditSchedule(
                request.type(), request.paymentDueDay(), request.closingDay());
        Account account = Account.builder()
                .id(UUID.randomUUID())
                .userId(user.userId())
                .name(request.name())
                .type(request.type())
                .currency(request.currency())
                .creditLimit(request.creditLimit())
                .currentBalance(
                        request.currentBalance() != null ? request.currentBalance() : BigDecimal.ZERO)
                .statementBalance(
                        request.statementBalance() != null ? request.statementBalance() : BigDecimal.ZERO)
                .paymentDueDay(schedule.paymentDueDay())
                .closingDay(schedule.closingDay())
                .archived(false)
                .createdAt(Instant.now())
                .build();
        accountRepository.save(account);
        return AccountResponse.from(account);
    }

    /**
     * Updates an existing account for the current user.
     */
    @Transactional
    public AccountResponse update(UUID id, AccountRequest request) {
        CurrentUser user = authFacade.currentUser();
        Account existing = accountRepository.findByIdAndUserId(id, user.userId())
                .orElseThrow(() -> new NotFoundException("Account not found"));
        CreditSchedule schedule = resolveCreditSchedule(
                request.type(), request.paymentDueDay(), request.closingDay());
        Account merged = Account.builder()
                .id(existing.getId())
                .userId(existing.getUserId())
                .name(request.name())
                .type(request.type())
                .currency(request.currency())
                .creditLimit(request.creditLimit())
                .currentBalance(
                        request.currentBalance() != null
                                ? request.currentBalance()
                                : existing.getCurrentBalance())
                .statementBalance(
                        request.statementBalance() != null
                                ? request.statementBalance()
                                : existing.getStatementBalance())
                .paymentDueDay(schedule.paymentDueDay())
                .closingDay(schedule.closingDay())
                .archived(existing.isArchived())
                .createdAt(existing.getCreatedAt())
                .build();
        accountRepository.update(merged);
        return AccountResponse.from(merged);
    }

    /**
     * Deletes an account by id for the current user.
     */
    @Transactional
    public void delete(UUID id) {
        CurrentUser user = authFacade.currentUser();
        if (!accountRepository.existsByIdAndUserId(id, user.userId())) {
            throw new NotFoundException("Account not found for current user");
        }
        accountRepository.deleteById(id);
    }

    private record CreditSchedule(Integer paymentDueDay, Integer closingDay) {
    }

    /**
     * Non-CREDIT accounts must not persist statement-cycle days (DB check constraints).
     * CREDIT accounts require both fields in 1..31.
     */
    private static CreditSchedule resolveCreditSchedule(
            AccountType type, Integer paymentDueDay, Integer closingDay) {
        if (type != AccountType.CREDIT) {
            return new CreditSchedule(null, null);
        }
        if (paymentDueDay == null || closingDay == null) {
            throw new IllegalArgumentException(
                    "CREDIT accounts require paymentDueDay and closingDay (each between 1 and 31)");
        }
        if (paymentDueDay < 1
                || paymentDueDay > 31
                || closingDay < 1
                || closingDay > 31) {
            throw new IllegalArgumentException("paymentDueDay and closingDay must be between 1 and 31");
        }
        return new CreditSchedule(paymentDueDay, closingDay);
    }
}
