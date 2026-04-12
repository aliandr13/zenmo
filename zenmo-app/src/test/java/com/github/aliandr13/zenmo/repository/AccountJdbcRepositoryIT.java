package com.github.aliandr13.zenmo.repository;

import com.github.aliandr13.zenmo.account.Account;
import com.github.aliandr13.zenmo.account.AccountType;
import java.math.BigDecimal;
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
class AccountJdbcRepositoryIT {

    private static final UUID USER_A = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID USER_B = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Autowired
    private AccountJdbcRepository repository;

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
        UUID accountId = UUID.randomUUID();
        Instant created = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        Account toSave = Account.builder()
                .id(accountId)
                .userId(USER_A)
                .name("Main Checking")
                .type(AccountType.CHECKING)
                .currency("USD")
                .creditLimit(null)
                .currentBalance(new BigDecimal("100.50"))
                .statementBalance(new BigDecimal("99.00"))
                .paymentDueDay(null)
                .closingDay(15)
                .archived(false)
                .createdAt(created)
                .build();

        repository.save(toSave);

        Optional<Account> found = repository.findByIdAndUserId(accountId, USER_A);
        assertThat(found).isPresent();
        Account a = found.get();
        assertThat(a.getId()).isEqualTo(accountId);
        assertThat(a.getUserId()).isEqualTo(USER_A);
        assertThat(a.getName()).isEqualTo("Main Checking");
        assertThat(a.getType()).isEqualTo(AccountType.CHECKING);
        assertThat(a.getCurrency()).isEqualTo("USD");
        assertThat(a.getCreditLimit()).isNull();
        assertThat(a.getCurrentBalance()).isEqualByComparingTo("100.50");
        assertThat(a.getStatementBalance()).isEqualByComparingTo("99.00");
        assertThat(a.getPaymentDueDay()).isNull();
        assertThat(a.getClosingDay()).isEqualTo(15);
        assertThat(a.isArchived()).isFalse();
        assertThat(a.getCreatedAt()).isEqualTo(created);
    }

    @Test
    void save_withCreditFields() {
        UUID accountId = UUID.randomUUID();
        Account toSave = Account.builder()
                .id(accountId)
                .userId(USER_A)
                .name("Visa")
                .type(AccountType.CREDIT)
                .currency("EUR")
                .creditLimit(new BigDecimal("5000.00"))
                .currentBalance(BigDecimal.ZERO)
                .statementBalance(new BigDecimal("-250.25"))
                .paymentDueDay(10)
                .closingDay(5)
                .archived(true)
                .createdAt(Instant.now())
                .build();

        repository.save(toSave);

        Account a = repository.findByIdAndUserId(accountId, USER_A).orElseThrow();
        assertThat(a.getCreditLimit()).isEqualByComparingTo("5000.00");
        assertThat(a.getCurrentBalance()).isEqualByComparingTo("0");
        assertThat(a.getStatementBalance()).isEqualByComparingTo("-250.25");
        assertThat(a.getPaymentDueDay()).isEqualTo(10);
        assertThat(a.getClosingDay()).isEqualTo(5);
        assertThat(a.isArchived()).isTrue();
        assertThat(a.getType()).isEqualTo(AccountType.CREDIT);
    }

    @Test
    void findByIdAndUserId_returnsEmptyWhenWrongUser() {
        UUID accountId = UUID.randomUUID();
        repository.save(sampleAccount(accountId, USER_A));

        assertThat(repository.findByIdAndUserId(accountId, USER_B)).isEmpty();
    }

    @Test
    void findByUserIdOrderByCreatedDesc_ordersNewestFirst_andFiltersByUser() {
        Instant older = Instant.parse("2024-01-01T12:00:00Z");
        Instant newer = Instant.parse("2024-06-01T12:00:00Z");
        UUID idOld = UUID.randomUUID();
        UUID idNew = UUID.randomUUID();

        repository.save(sampleAccountBuilder(idOld, USER_A).name("Old").createdAt(older).build());
        repository.save(sampleAccountBuilder(idNew, USER_A).name("New").createdAt(newer).build());

        UUID otherUserAccount = UUID.randomUUID();
        repository.save(sampleAccountBuilder(otherUserAccount, USER_B).name("B only").createdAt(newer).build());

        List<Account> forA = repository.findByUserIdOrderByCreatedDesc(USER_A);
        assertThat(forA).extracting(Account::getName).containsExactly("New", "Old");

        List<Account> forB = repository.findByUserIdOrderByCreatedDesc(USER_B);
        assertThat(forB).hasSize(1);
        assertThat(forB.getFirst().getName()).isEqualTo("B only");
    }

    @Test
    void existsByIdAndUserId() {
        UUID accountId = UUID.randomUUID();
        repository.save(sampleAccount(accountId, USER_A));

        assertThat(repository.existsByIdAndUserId(accountId, USER_A)).isTrue();
        assertThat(repository.existsByIdAndUserId(accountId, USER_B)).isFalse();
        assertThat(repository.existsByIdAndUserId(UUID.randomUUID(), USER_A)).isFalse();
    }

    @Test
    void update_persistsChanges() {
        UUID accountId = UUID.randomUUID();
        Account saved = sampleAccountBuilder(accountId, USER_A)
                .name("Before")
                .type(AccountType.CHECKING)
                .currency("USD")
                .build();
        repository.save(saved);

        Account merged = Account.builder()
                .id(accountId)
                .userId(USER_A)
                .name("After")
                .type(AccountType.SAVINGS)
                .currency("GBP")
                .creditLimit(new BigDecimal("1000.00"))
                .currentBalance(new BigDecimal("25.00"))
                .statementBalance(new BigDecimal("30.00"))
                .paymentDueDay(null)
                .closingDay(1)
                .archived(false)
                .createdAt(saved.getCreatedAt())
                .build();
        repository.update(merged);

        Account a = repository.findByIdAndUserId(accountId, USER_A).orElseThrow();
        assertThat(a.getName()).isEqualTo("After");
        assertThat(a.getType()).isEqualTo(AccountType.SAVINGS);
        assertThat(a.getCurrency()).isEqualTo("GBP");
        assertThat(a.getCreditLimit()).isEqualByComparingTo("1000.00");
        assertThat(a.getCurrentBalance()).isEqualByComparingTo("25.00");
        assertThat(a.getStatementBalance()).isEqualByComparingTo("30.00");
        assertThat(a.getCreatedAt().truncatedTo(ChronoUnit.MICROS))
                .isEqualTo(saved.getCreatedAt().truncatedTo(ChronoUnit.MICROS));
    }

    @Test
    void deleteById_removesRow() {
        UUID accountId = UUID.randomUUID();
        repository.save(sampleAccount(accountId, USER_A));

        repository.deleteById(accountId);

        assertThat(repository.findByIdAndUserId(accountId, USER_A)).isEmpty();
    }

    private Account sampleAccount(UUID id, UUID userId) {
        return sampleAccountBuilder(id, userId).build();
    }

    private Account.AccountBuilder sampleAccountBuilder(UUID id, UUID userId) {
        return Account.builder()
                .id(id)
                .userId(userId)
                .name("Acc")
                .type(AccountType.CASH)
                .currency("USD")
                .creditLimit(null)
                .currentBalance(BigDecimal.ZERO)
                .statementBalance(BigDecimal.ZERO)
                .paymentDueDay(null)
                .closingDay(1)
                .archived(false)
                .createdAt(Instant.now());
    }
}
