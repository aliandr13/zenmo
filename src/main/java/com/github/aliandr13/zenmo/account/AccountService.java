package com.github.aliandr13.zenmo.account;

import com.github.aliandr13.zenmo.account.dto.AccountRequest;
import com.github.aliandr13.zenmo.account.dto.AccountResponse;
import com.github.aliandr13.zenmo.common.NotFoundException;
import com.github.aliandr13.zenmo.security.AuthFacade;
import com.github.aliandr13.zenmo.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final AuthFacade authFacade;

    public AccountService(AccountRepository accountRepository, AuthFacade authFacade) {
        this.accountRepository = accountRepository;
        this.authFacade = authFacade;
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> list() {
        CurrentUser user = authFacade.currentUser();
        return accountRepository.findByUserIdOrderByCreatedAtDesc(user.userId())
                .stream()
                .map(AccountResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AccountResponse get(UUID id) {
        CurrentUser user = authFacade.currentUser();
        Account account = accountRepository.findByIdAndUserId(id, user.userId())
                .orElseThrow(() -> new NotFoundException("Account not found"));
        return AccountResponse.from(account);
    }

    @Transactional
    public AccountResponse create(AccountRequest request) {
        CurrentUser user = authFacade.currentUser();
        Account account = new Account(
                UUID.randomUUID(),
                user.userId(),
                request.name(),
                request.type(),
                request.currency(),
                request.creditLimit(),
                false,
                Instant.now()
        );
        accountRepository.save(account);
        return AccountResponse.from(account);
    }

    @Transactional
    public void delete(UUID id) {
        CurrentUser user = authFacade.currentUser();
        if (!accountRepository.existsByIdAndUserId(id, user.userId())) {
            throw new NotFoundException("Account not found");
        }
        accountRepository.deleteById(id);
    }
}
