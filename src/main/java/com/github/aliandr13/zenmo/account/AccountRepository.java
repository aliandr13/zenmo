package com.github.aliandr13.zenmo.account;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * JPA repository for accounts.
 */
public interface AccountRepository extends JpaRepository<Account, UUID> {

    /**
     * Returns accounts for a user, newest first.
     */
    List<Account> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * Finds an account by id and owner user.
     */
    Optional<Account> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Returns whether an account exists for the given id and user.
     */
    boolean existsByIdAndUserId(UUID id, UUID userId);
}
