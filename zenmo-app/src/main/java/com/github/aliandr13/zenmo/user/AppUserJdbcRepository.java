package com.github.aliandr13.zenmo.user;

import com.github.aliandr13.zenmo.utils.CollectionUtils;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * JDBC implementation of AppUserRepository.
 */
@Repository
@RequiredArgsConstructor
public class AppUserJdbcRepository implements AppUserRepository {

    private static final String GET_BY_EMAIL = "SELECT * FROM app_user WHERE email = ?";
    private static final String COUNT_BY_EMAIL = "SELECT COUNT(*) FROM app_user WHERE email = ?";
    private static final String GET_BY_ID = "SELECT * FROM app_user WHERE id = ?";
    private static final String INSERT =
            "INSERT INTO app_user (id, email, password_hash, created_at) VALUES (?, ?, ?, ?)";

    private final JdbcTemplate jdbc;

    @Override
    public Optional<AppUser> findByEmail(String email) {
        List<AppUser> list = jdbc.query(GET_BY_EMAIL, ROW_MAPPER, email);
        return CollectionUtils.optionalFirst(list);
    }

    @Override
    public boolean existsByEmail(String email) {
        Long count = jdbc.queryForObject(COUNT_BY_EMAIL, Long.class, email);
        return count != null && count > 0;
    }

    @Override
    public Optional<AppUser> findById(UUID id) {
        List<AppUser> list = jdbc.query(GET_BY_ID, ROW_MAPPER, id);
        return CollectionUtils.optionalFirst(list);
    }

    @Override
    public void save(AppUser user) {
        jdbc.update(INSERT,
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                Timestamp.from(user.getCreatedAt())
        );
    }

    private static final RowMapper<AppUser> ROW_MAPPER = (rs, rowNum) -> new AppUser(
            UUID.fromString(rs.getString("id")),
            rs.getString("email"),
            rs.getString("password_hash"),
            rs.getTimestamp("created_at").toInstant());

}
