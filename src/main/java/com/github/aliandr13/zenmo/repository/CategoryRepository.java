package com.github.aliandr13.zenmo.repository;

import com.github.aliandr13.zenmo.category.Category;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for categories.
 */
public interface CategoryRepository {

    /**
     * Returns categories for a user, ordered by name.
     */
    List<Category> findByUserIdOrderByName(UUID userId);

    /**
     * Finds a category by id and owner user.
     */
    Optional<Category> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Returns whether a category exists for the given id and user.
     */
    boolean existsByIdAndUserId(UUID id, UUID userId);

    /**
     * Returns whether a category with the given name exists for the user.
     */
    boolean existsByUserIdAndNameIgnoreCase(UUID userId, String name);

    /**
     * Saves a category.
     */
    void save(Category category);

    /**
     * Deletes a category by id.
     */
    void deleteById(UUID id);
}
