package com.github.aliandr13.zenmo.repository;

import com.github.aliandr13.zenmo.account.Account;
import java.math.BigDecimal;
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
     * Updates an existing account (row must belong to the account's user).
     */
    void update(Account account);

    /**
     * Atomically adds {@code delta} to the account's current balance (negative delta decreases it).
     *
     * @return number of rows updated (0 when the account does not exist for the user)
     */
    int adjustBalance(UUID id, UUID userId, BigDecimal delta);

    /**
     * Deletes an account by id.
     */
    void deleteById(UUID id);
}
