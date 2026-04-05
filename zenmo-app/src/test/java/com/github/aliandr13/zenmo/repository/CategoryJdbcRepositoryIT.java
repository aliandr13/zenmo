package com.github.aliandr13.zenmo.repository;

import com.github.aliandr13.zenmo.category.Category;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CategoryJdbcRepositoryIT {

    private static final UUID USER_A = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID USER_B = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");

    @Autowired
    private CategoryJdbcRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        insertUser(USER_A);
        insertUser(USER_B);
    }

    private void insertUser(UUID id) {
        jdbcTemplate.update(
                "INSERT INTO app_user (id, email, password_hash, created_at) VALUES (?, ?, ?, ?)",
                id.toString(),
                id + "@test.example",
                "hash",
                Timestamp.from(Instant.now()));
    }

    @Test
    void saveAndFindByIdAndUserId_roundTrip() {
        UUID categoryId = UUID.randomUUID();
        Instant created = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        Category toSave = Category.builder()
                .id(categoryId)
                .userId(USER_A)
                .name("Groceries")
                .parentId(null)
                .color("#00aa00")
                .createdAt(created)
                .build();

        repository.save(toSave);

        Optional<Category> found = repository.findByIdAndUserId(categoryId, USER_A);
        assertThat(found).isPresent();
        Category c = found.get();
        assertThat(c.getId()).isEqualTo(categoryId);
        assertThat(c.getUserId()).isEqualTo(USER_A);
        assertThat(c.getName()).isEqualTo("Groceries");
        assertThat(c.getParentId()).isNull();
        assertThat(c.getColor()).isEqualTo("#00aa00");
        assertThat(c.getCreatedAt()).isEqualTo(created);
    }

    @Test
    void save_withParentId_andNullColor() {
        UUID parentId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();
        repository.save(sampleCategoryBuilder(parentId, USER_A).name("Parent").parentId(null).color(null).build());
        repository.save(sampleCategoryBuilder(childId, USER_A).name("Child").parentId(parentId).color(null).build());

        Category child = repository.findByIdAndUserId(childId, USER_A).orElseThrow();
        assertThat(child.getParentId()).isEqualTo(parentId);
        assertThat(child.getColor()).isNull();
    }

    @Test
    void findByIdAndUserId_returnsEmptyWhenWrongUser() {
        UUID categoryId = UUID.randomUUID();
        repository.save(sampleCategory(categoryId, USER_A));

        assertThat(repository.findByIdAndUserId(categoryId, USER_B)).isEmpty();
    }

    @Test
    void findByUserIdOrderByName_sortsByName_andFiltersByUser() {
        UUID idB = UUID.randomUUID();
        UUID idA = UUID.randomUUID();
        repository.save(sampleCategoryBuilder(idB, USER_A).name("Beta").build());
        repository.save(sampleCategoryBuilder(idA, USER_A).name("Alpha").build());

        UUID otherUserCat = UUID.randomUUID();
        repository.save(sampleCategoryBuilder(otherUserCat, USER_B).name("Alpha").build());

        List<Category> forA = repository.findByUserIdOrderByName(USER_A);
        assertThat(forA).extracting(Category::getName).containsExactly("Alpha", "Beta");

        List<Category> forB = repository.findByUserIdOrderByName(USER_B);
        assertThat(forB).hasSize(1);
        assertThat(forB.getFirst().getName()).isEqualTo("Alpha");
    }

    @Test
    void existsByIdAndUserId() {
        UUID categoryId = UUID.randomUUID();
        repository.save(sampleCategory(categoryId, USER_A));

        assertThat(repository.existsByIdAndUserId(categoryId, USER_A)).isTrue();
        assertThat(repository.existsByIdAndUserId(categoryId, USER_B)).isFalse();
        assertThat(repository.existsByIdAndUserId(UUID.randomUUID(), USER_A)).isFalse();
    }

    @Test
    void existsByUserIdAndNameIgnoreCase() {
        UUID categoryId = UUID.randomUUID();
        repository.save(sampleCategoryBuilder(categoryId, USER_A).name("Food").build());

        assertThat(repository.existsByUserIdAndNameIgnoreCase(USER_A, "food")).isTrue();
        assertThat(repository.existsByUserIdAndNameIgnoreCase(USER_A, "FOOD")).isTrue();
        assertThat(repository.existsByUserIdAndNameIgnoreCase(USER_A, "Travel")).isFalse();
        assertThat(repository.existsByUserIdAndNameIgnoreCase(USER_B, "Food")).isFalse();
    }

    @Test
    void deleteById_removesRow() {
        UUID categoryId = UUID.randomUUID();
        repository.save(sampleCategory(categoryId, USER_A));

        repository.deleteById(categoryId);

        assertThat(repository.findByIdAndUserId(categoryId, USER_A)).isEmpty();
    }

    private Category sampleCategory(UUID id, UUID userId) {
        return sampleCategoryBuilder(id, userId).build();
    }

    private Category.CategoryBuilder sampleCategoryBuilder(UUID id, UUID userId) {
        return Category.builder()
                .id(id)
                .userId(userId)
                .name("Cat")
                .parentId(null)
                .color("#fff")
                .createdAt(Instant.now());
    }
}
