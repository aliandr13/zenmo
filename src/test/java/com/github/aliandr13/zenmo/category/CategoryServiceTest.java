package com.github.aliandr13.zenmo.category;

import com.github.aliandr13.zenmo.category.dto.CategoryRequest;
import com.github.aliandr13.zenmo.category.dto.CategoryResponse;
import com.github.aliandr13.zenmo.common.NotFoundException;
import com.github.aliandr13.zenmo.security.AuthFacade;
import com.github.aliandr13.zenmo.security.CurrentUser;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private AuthFacade authFacade;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void listReturnsCategoriesForCurrentUser() {
        UUID userId = UUID.randomUUID();
        given(authFacade.currentUser()).willReturn(new CurrentUser(userId, "user@example.com"));

        Category category = new Category(UUID.randomUUID(), userId, "Food", null, "#ffffff", Instant.now());
        given(categoryRepository.findByUserIdOrderByName(userId)).willReturn(List.of(category));

        List<CategoryResponse> result = categoryService.list();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().name()).isEqualTo("Food");
    }

    @Test
    void getReturnsCategoryForCurrentUser() {
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        given(authFacade.currentUser()).willReturn(new CurrentUser(userId, "user@example.com"));

        Category category = new Category(categoryId, userId, "Food", null, "#ffffff", Instant.now());
        given(categoryRepository.findByIdAndUserId(categoryId, userId)).willReturn(Optional.of(category));

        CategoryResponse response = categoryService.get(categoryId);

        assertThat(response.id()).isEqualTo(categoryId);
        assertThat(response.name()).isEqualTo("Food");
    }

    @Test
    void getThrowsWhenCategoryNotFound() {
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        given(authFacade.currentUser()).willReturn(new CurrentUser(userId, "user@example.com"));
        given(categoryRepository.findByIdAndUserId(categoryId, userId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.get(categoryId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Category not found");
    }

    @Test
    void createPersistsCategoryWhenNameNotExistsForUser() {
        UUID userId = UUID.randomUUID();
        given(authFacade.currentUser()).willReturn(new CurrentUser(userId, "user@example.com"));
        given(categoryRepository.existsByUserIdAndNameIgnoreCase(userId, "Food")).willReturn(false);

        CategoryRequest request = new CategoryRequest("Food", null, "#ffffff");

        CategoryResponse response = categoryService.create(request);

        assertThat(response.name()).isEqualTo("Food");
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void createThrowsWhenNameAlreadyExistsForUser() {
        UUID userId = UUID.randomUUID();
        given(authFacade.currentUser()).willReturn(new CurrentUser(userId, "user@example.com"));
        given(categoryRepository.existsByUserIdAndNameIgnoreCase(userId, "Food")).willReturn(true);

        CategoryRequest request = new CategoryRequest("Food", null, "#ffffff");

        assertThatThrownBy(() -> categoryService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void deleteDeletesWhenCategoryExistsForUser() {
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        given(authFacade.currentUser()).willReturn(new CurrentUser(userId, "user@example.com"));
        given(categoryRepository.existsByIdAndUserId(categoryId, userId)).willReturn(true);

        categoryService.delete(categoryId);

        verify(categoryRepository).deleteById(eq(categoryId));
    }

    @Test
    void deleteThrowsWhenCategoryNotFoundForUser() {
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        given(authFacade.currentUser()).willReturn(new CurrentUser(userId, "user@example.com"));
        given(categoryRepository.existsByIdAndUserId(categoryId, userId)).willReturn(false);

        assertThatThrownBy(() -> categoryService.delete(categoryId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Category not found");
    }
}

