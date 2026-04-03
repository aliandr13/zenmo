package com.github.aliandr13.zenmo.repository;

import com.github.aliandr13.zenmo.account.Account;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for accounts.
 */
public interface AccountRepository {

    /**
     * Returns accounts for a user, newest first.
     */
    List<Account> findByUserIdOrderByCreatedDesc(UUID userId);

    /**
     * Finds an account by id and owner user.
     */
    Optional<Account> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Returns whether an account exists for the given id and user.
     */
    boolean existsByIdAndUserId(UUID id, UUID userId);

    /**
     * Saves an account.
     */
    void save(Account account);

    /**
     * Deletes an account by id.
     */
    void deleteById(UUID id);
}
