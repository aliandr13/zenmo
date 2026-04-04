package com.github.aliandr13.zenmo.user;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * PostgreSQL JDBC cannot infer a SQL type for {@link Instant} when Spring passes it to
 * {@code setObject}; {@link AppUserJdbcRepository} must bind {@code created_at} as
 * {@link Timestamp}. Regression test for register failing with PSQLException in production.
 */
@ExtendWith(MockitoExtension.class)
class AppUserJdbcRepositorySaveBindingTest {

    private static final String INSERT_SQL =
            "INSERT INTO app_user (id, email, password_hash, created_at) VALUES (?, ?, ?, ?)";

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private AppUserJdbcRepository repository;

    @Test
    void saveBindsCreatedAtAsSqlTimestamp() {
        UUID id = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Instant created = Instant.parse("2026-04-03T21:46:47Z");
        AppUser user = new AppUser(id, "test@email.com", "hash", created);

        repository.save(user);

        verify(jdbcTemplate).update(
                eq(INSERT_SQL),
                eq(id),
                eq("test@email.com"),
                eq("hash"),
                eq(Timestamp.from(created)));
    }
}
