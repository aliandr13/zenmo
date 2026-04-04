package com.github.aliandr13.zenmo.repository;

import com.github.aliandr13.zenmo.account.Account;
import com.github.aliandr13.zenmo.account.AccountType;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * JDBC implementation of AccountRepository.
 */
@Repository
@RequiredArgsConstructor
public class AccountJdbcRepository implements AccountRepository {

    private static final String GET_BY_USER_ID = "SELECT * FROM account WHERE user_id = ? ORDER BY created_at DESC";
    private static final String GET_BY_ID_AND_USER = "SELECT * FROM account WHERE id = ? AND user_id = ?";
    private static final String COUNT_BY_ID_AND_USER_ID = "SELECT COUNT(id) FROM account WHERE id = ? AND user_id = ?";
    private static final String DELETE_BY_ID = "DELETE FROM account WHERE id = ?";
    private static final String INSERT =
            "INSERT INTO account (id, user_id, name, type, currency, credit_limit, payment_due_day, closing_day, archived, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<Account> findByUserIdOrderByCreatedDesc(final UUID userId) {
        return jdbcTemplate.query(GET_BY_USER_ID, ROW_MAPPER, userId);
    }

    @Override
    public Optional<Account> findByIdAndUserId(UUID id, UUID userId) {
        List<Account> list = jdbcTemplate.query(GET_BY_ID_AND_USER, ROW_MAPPER, id, userId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.getFirst());
    }

    @Override
    public boolean existsByIdAndUserId(UUID id, UUID userId) {
        Long count = jdbcTemplate.queryForObject(COUNT_BY_ID_AND_USER_ID, Long.class, id, userId);
        return count != null && count > 0;
    }

    @Override
    public void save(Account account) {
        jdbcTemplate.update(INSERT,
                account.getId(),
                account.getUserId(),
                account.getName(),
                account.getType().name(),
                account.getCurrency(),
                account.getCreditLimit(),
                account.getPaymentDueDay(),
                account.getClosingDay(),
                account.isArchived(),
                Timestamp.from(account.getCreatedAt())
        );
    }

    @Override
    public void deleteById(UUID id) {
        jdbcTemplate.update(DELETE_BY_ID, id);
    }

    private static final RowMapper<Account> ROW_MAPPER = (rs, rowNum) -> Account.builder()
            .id(UUID.fromString(rs.getString("id")))
            .userId(UUID.fromString(rs.getString("user_id")))
            .name(rs.getString("name"))
            .type(AccountType.valueOf(rs.getString("type")))
            .currency(rs.getString("currency"))
            .creditLimit(rs.getObject("credit_limit", BigDecimal.class))
            .paymentDueDay(rs.getObject("payment_due_day", Integer.class))
            .closingDay(rs.getObject("closing_day", Integer.class))
            .archived(rs.getBoolean("archived"))
            .createdAt(rs.getTimestamp("created_at").toInstant())
            .build();
}
