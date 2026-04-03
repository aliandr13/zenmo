package com.github.aliandr13.zenmo.auth;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * JDBC implementation of RefreshTokenRepository.
 */
@Repository
@RequiredArgsConstructor
public class RefreshTokenJdbcRepository implements RefreshTokenRepository {

    private static final String GET_TOKEN = "SELECT * FROM refresh_token WHERE token_hash = ?";
    private static final String DELETE_BY_DATE = "DELETE FROM refresh_token WHERE expires_at < ?";
    private static final String DELETE_BY_ID = "DELETE FROM refresh_token WHERE id = ?";
    private static final String INSERT =
            "INSERT INTO refresh_token (id, user_id, token_hash, expires_at, revoked_at, created_at) VALUES (?, ?, ?, ?, ?, ?)";

    private final JdbcTemplate jdbc;

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        List<RefreshToken> list = jdbc.query(GET_TOKEN, ROW_MAPPER, tokenHash);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.getFirst());
    }

    @Override
    public long deleteByExpiresAtBefore(Instant instant) {
        return jdbc.update(DELETE_BY_DATE, Timestamp.from(instant));
    }

    @Override
    public void save(RefreshToken token) {
        jdbc.update(INSERT,
                token.getId(),
                token.getUserId(),
                token.getTokenHash(),
                Timestamp.from(token.getExpiresAt()),
                token.getRevokedAt() == null ? null : Timestamp.from(token.getRevokedAt()),
                Timestamp.from(token.getCreatedAt()));
    }

    @Override
    public void delete(RefreshToken token) {
        jdbc.update(DELETE_BY_ID, token.getId());
    }

    private static final RowMapper<RefreshToken> ROW_MAPPER = (rs, rowNum) -> {
        Instant revokedAt = rs.getTimestamp("revoked_at") == null
                ? null : rs.getTimestamp("revoked_at").toInstant();
        return new RefreshToken(
                UUID.fromString(rs.getString("id")),
                UUID.fromString(rs.getString("user_id")),
                rs.getString("token_hash"),
                rs.getTimestamp("expires_at").toInstant(),
                revokedAt,
                rs.getTimestamp("created_at").toInstant());
    };
}
