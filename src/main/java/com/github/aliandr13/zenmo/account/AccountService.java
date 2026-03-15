package com.github.aliandr13.zenmo.account;

import com.github.aliandr13.zenmo.account.dto.AccountRequest;
import com.github.aliandr13.zenmo.account.dto.AccountResponse;
import com.github.aliandr13.zenmo.common.NotFoundException;
import com.github.aliandr13.zenmo.security.AuthFacade;
import com.github.aliandr13.zenmo.security.CurrentUser;
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
        return accountRepository.findByUserIdOrderByCreatedAtDesc(user.userId())
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
        Account account = Account.builder()
                .id(UUID.randomUUID())
                .userId(user.userId())
                .name(request.name())
                .type(request.type())
                .currency(request.currency())
                .creditLimit(request.creditLimit())
                .archived(false)
                .createdAt(Instant.now())
                .build();
        accountRepository.save(account);
        return AccountResponse.from(account);
    }

    /**
     * Deletes an account by id for the current user.
     */
    @Transactional
    public void delete(UUID id) {
        CurrentUser user = authFacade.currentUser();
        if (!accountRepository.existsByIdAndUserId(id, user.userId())) {
            throw new NotFoundException("Account not found");
        }
        accountRepository.deleteById(id);
    }
}
