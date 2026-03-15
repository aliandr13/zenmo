package com.github.aliandr13.zenmo.user;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * JPA repository for users.
 */
public interface AppUserRepository extends JpaRepository<AppUser, UUID> {
    /**
     * Finds a user by email (case-insensitive).
     */
    Optional<AppUser> findByEmailIgnoreCase(String email);

    /**
     * Returns whether a user exists with the given email (case-insensitive).
     */
    boolean existsByEmailIgnoreCase(String email);
}
