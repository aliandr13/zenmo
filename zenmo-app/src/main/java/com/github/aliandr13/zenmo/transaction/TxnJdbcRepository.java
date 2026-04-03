package com.github.aliandr13.zenmo.transaction;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * JDBC implementation of TxnRepository.
 */
@Repository
@RequiredArgsConstructor
public class TxnJdbcRepository implements TxnRepository {

    private static final String DELETE_BY_ID = "DELETE FROM txn WHERE id = ?";
    private static final String COUNT_BY_ID_AND_USER = "SELECT COUNT(*) FROM txn WHERE id = ? AND user_id = ?";
    private static final String GET_BY_ID_AND_USER = "SELECT * FROM txn WHERE id = ? AND user_id = ?";
    private static final String GET_BY_USER_AND_DATE = "SELECT * FROM txn WHERE user_id = ? AND transaction_date BETWEEN ? AND ?";
    private static final String COUNT_BY_USER_AND_DATE = "SELECT COUNT(1) FROM txn WHERE user_id = ? AND transaction_date BETWEEN ? AND ?";
    private static final String GET_BY_USER =
            "SELECT * FROM txn WHERE user_id = ? ORDER BY transaction_date DESC, created_at DESC LIMIT ? OFFSET ?";
    private static final String COUNT_BY_USER = "SELECT COUNT(1) FROM txn WHERE user_id = ?";
    private static final String GET_BY_USER_AND_ACCOUNT =
            "SELECT * FROM txn WHERE user_id = ? AND account_id = ? ORDER BY transaction_date DESC, created_at DESC LIMIT ? OFFSET ?";
    private static final String COUNT_BY_USER_AND_ACCOUNT =
            "SELECT COUNT(1) FROM txn WHERE user_id = ? AND account_id = ?";
    private static final String INSERT =
            "INSERT INTO txn (id, user_id, account_id, category_id, transaction_date, post_date, amount, currency, description, merchant, status, notes, created_at)VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Page<Txn> findByUserIdOrderByTransactionDateDescCreatedAtDesc(UUID userId, Pageable pageable) {
        return paged(GET_BY_USER, COUNT_BY_USER, pageable, userId);
    }

    @Override
    public Page<Txn> findByUserIdAndAccountIdOrderByTransactionDateDescCreatedAtDesc(
            UUID userId, UUID accountId, Pageable pageable) {
        return paged(GET_BY_USER_AND_ACCOUNT, COUNT_BY_USER_AND_ACCOUNT, pageable, userId, accountId);
    }

    @Override
    public Page<Txn> findByUserIdAndTransactionDateBetweenOrderByTransactionDateDescCreatedAtDesc(
            UUID userId, LocalDate from, LocalDate to, Pageable pageable) {
        return paged(GET_BY_USER_AND_DATE, COUNT_BY_USER_AND_DATE, pageable, userId, from, to);
    }

    @Override
    public Optional<Txn> findByIdAndUserId(UUID id, UUID userId) {
        List<Txn> list = jdbcTemplate.query(GET_BY_ID_AND_USER, ROW_MAPPER, id, userId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.getFirst());
    }

    @Override
    public boolean existsByIdAndUserId(UUID id, UUID userId) {
        Long count = jdbcTemplate.queryForObject(COUNT_BY_ID_AND_USER, Long.class, id, userId);
        return count != null && count > 0;
    }

    @Override
    public void save(Txn txn) {
        jdbcTemplate.update(INSERT,
                txn.getId(),
                txn.getUserId(),
                txn.getAccountId(),
                txn.getCategoryId(),
                txn.getTransactionDate(),
                txn.getPostDate(),
                txn.getAmount(),
                txn.getCurrency(),
                txn.getDescription(),
                txn.getMerchant(),
                txn.getStatus().name(),
                txn.getNotes(),
                txn.getCreatedAt());
    }

    @Override
    public void deleteById(UUID id) {
        jdbcTemplate.update(DELETE_BY_ID, id);
    }

    private Page<Txn> paged(String selectSql, String countSql, Pageable pageable, Object... params) {
        int page = pageable.getPageNumber();
        int size = pageable.getPageSize();
        Long total = jdbcTemplate.queryForObject(countSql, Long.class, params);
        if (total == null || total == 0) {
            return new PageImpl<>(List.of(), PageRequest.of(page, size), 0);
        }
        Object[] queryParams = new Object[params.length + 2];
        System.arraycopy(params, 0, queryParams, 0, params.length);
        queryParams[params.length] = size;
        queryParams[params.length + 1] = (long) page * size;
        List<Txn> content = jdbcTemplate.query(selectSql, ROW_MAPPER, queryParams);
        return new PageImpl<>(content, PageRequest.of(page, size), total);
    }

    private static final RowMapper<Txn> ROW_MAPPER = (rs, rowNum) -> {
        String catId = rs.getString("category_id");
        java.sql.Date postDate = rs.getDate("post_date");
        return Txn.builder()
                .id(UUID.fromString(rs.getString("id")))
                .userId(UUID.fromString(rs.getString("user_id")))
                .accountId(UUID.fromString(rs.getString("account_id")))
                .categoryId(catId == null ? null : UUID.fromString(catId))
                .transactionDate(rs.getDate("transaction_date").toLocalDate())
                .postDate(postDate == null ? null : postDate.toLocalDate())
                .amount(rs.getBigDecimal("amount"))
                .currency(rs.getString("currency"))
                .description(rs.getString("description"))
                .merchant(rs.getString("merchant"))
                .status(TransactionStatus.valueOf(rs.getString("status")))
                .notes(rs.getString("notes"))
                .createdAt(rs.getTimestamp("created_at").toInstant())
                .build();
    };
}
