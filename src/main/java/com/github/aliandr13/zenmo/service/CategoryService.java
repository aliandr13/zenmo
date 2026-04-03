package com.github.aliandr13.zenmo.service;

import com.github.aliandr13.zenmo.category.Category;
import com.github.aliandr13.zenmo.category.dto.CategoryRequest;
import com.github.aliandr13.zenmo.category.dto.CategoryResponse;
import com.github.aliandr13.zenmo.common.NotFoundException;
import com.github.aliandr13.zenmo.repository.CategoryRepository;
import com.github.aliandr13.zenmo.security.AuthFacade;
import com.github.aliandr13.zenmo.security.CurrentUser;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service for category operations.
 */
@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final AuthFacade authFacade;

    /**
     * Constructor.
     */
    public CategoryService(CategoryRepository categoryRepository, AuthFacade authFacade) {
        this.categoryRepository = categoryRepository;
        this.authFacade = authFacade;
    }

    /**
     * Returns all categories for the current user.
     */
    @Transactional(readOnly = true)
    public List<CategoryResponse> list() {
        CurrentUser user = authFacade.currentUser();
        return categoryRepository.findByUserIdOrderByName(user.userId())
                .stream()
                .map(CategoryResponse::from)
                .toList();
    }

    /**
     * Returns a single category by id for the current user.
     */
    @Transactional(readOnly = true)
    public CategoryResponse get(UUID id) {
        CurrentUser user = authFacade.currentUser();
        Category category = categoryRepository.findByIdAndUserId(id, user.userId())
                .orElseThrow(() -> new NotFoundException("Category not found"));
        return CategoryResponse.from(category);
    }

    /**
     * Creates a new category for the current user.
     */
    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        CurrentUser user = authFacade.currentUser();
        if (categoryRepository.existsByUserIdAndNameIgnoreCase(user.userId(), request.name())) {
            throw new IllegalArgumentException("Category with this name already exists");
        }
        Category category = new Category(
                UUID.randomUUID(),
                user.userId(),
                request.name(),
                request.parentId(),
                request.color(),
                Instant.now()
        );
        categoryRepository.save(category);
        return CategoryResponse.from(category);
    }

    /**
     * Deletes a category by id for the current user.
     */
    @Transactional
    public void delete(UUID id) {
        CurrentUser user = authFacade.currentUser();
        if (!categoryRepository.existsByIdAndUserId(id, user.userId())) {
            throw new NotFoundException("Category not found");
        }
        categoryRepository.deleteById(id);
    }
}
