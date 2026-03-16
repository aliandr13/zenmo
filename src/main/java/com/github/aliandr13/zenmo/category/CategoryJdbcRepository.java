package com.github.aliandr13.zenmo.category;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * JDBC implementation of CategoryRepository.
 */
@Repository
@RequiredArgsConstructor
public class CategoryJdbcRepository implements CategoryRepository {

    private static final String DELETE_BY_ID = "DELETE FROM category WHERE id = ?";
    private static final String INSERT = "INSERT INTO category (id, user_id, name, parent_id, color, created_at) VALUES (?, ?, ?, ?, ?, ?)";
    private static final String COUNT_BY_USER_ID_AND_NAME = "SELECT COUNT(1) FROM category WHERE user_id = ? AND LOWER(name) = LOWER(?)";
    private static final String COUNT_BY_ID_AND_USER = "SELECT COUNT(id) FROM category WHERE id = ? AND user_id = ?";
    private static final String GET_BY_ID_AND_USER = "SELECT * WHERE id = ? AND user_id = ?";
    private static final String GET_BY_USERAND = "SELECT * FROM category WHERE user_id = ? ORDER BY name";

    private final JdbcTemplate jdbc;

    @Override
    public List<Category> findByUserIdOrderByName(UUID userId) {
        return jdbc.query(GET_BY_USERAND, ROW_MAPPER, userId);
    }

    @Override
    public Optional<Category> findByIdAndUserId(UUID id, UUID userId) {
        List<Category> list = jdbc.query(GET_BY_ID_AND_USER, ROW_MAPPER, id, userId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.getFirst());
    }

    @Override
    public boolean existsByIdAndUserId(UUID id, UUID userId) {
        Long count = jdbc.queryForObject(COUNT_BY_ID_AND_USER, Long.class, id, userId);
        return count != null && count > 0;
    }

    @Override
    public boolean existsByUserIdAndNameIgnoreCase(UUID userId, String name) {
        Long count = jdbc.queryForObject(COUNT_BY_USER_ID_AND_NAME, Long.class, userId, name);
        return count != null && count > 0;
    }

    @Override
    public void save(Category category) {
        jdbc.update(INSERT,
                category.getId(),
                category.getUserId(),
                category.getName(),
                category.getParentId(),
                category.getColor(),
                category.getCreatedAt());
    }

    @Override
    public void deleteById(UUID id) {
        jdbc.update(DELETE_BY_ID, id);
    }

    private static final RowMapper<Category> ROW_MAPPER = (rs, rowNum) -> {
        String parentIdStr = rs.getString("parent_id");
        return Category.builder()
                .id(UUID.fromString(rs.getString("id")))
                .userId(UUID.fromString(rs.getString("user_id")))
                .name(rs.getString("name"))
                .parentId(parentIdStr == null ? null : UUID.fromString(parentIdStr))
                .color(rs.getString("color"))
                .createdAt(rs.getTimestamp("created_at").toInstant())
                .build();
    };
}
