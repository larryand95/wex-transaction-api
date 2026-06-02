package com.wexinc.transaction.repository;

import com.wexinc.transaction.PostgreSQLContainerConfig;
import com.wexinc.transaction.domain.entity.PurchaseTransaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(PostgreSQLContainerConfig.class)
@ActiveProfiles("test")
@DisplayName("PurchaseTransactionRepository Tests")
class PurchaseTransactionRepositoryTest {

    @Autowired
    private PurchaseTransactionRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("should save and retrieve a transaction by id")
    void shouldSaveAndRetrieveTransaction() {
        PurchaseTransaction transaction = PurchaseTransaction.builder()
                .description("Test purchase")
                .transactionDate(LocalDate.of(2024, 6, 15))
                .purchaseAmount(new BigDecimal("123.45"))
                .build();

        PurchaseTransaction saved = repository.saveAndFlush(transaction);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();

        Optional<PurchaseTransaction> found = repository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getDescription()).isEqualTo("Test purchase");
        assertThat(found.get().getTransactionDate()).isEqualTo(LocalDate.of(2024, 6, 15));
        assertThat(found.get().getPurchaseAmount()).isEqualByComparingTo("123.45");
    }

    @Test
    @DisplayName("should generate unique UUID for each transaction")
    void shouldGenerateUniqueIds() {
        PurchaseTransaction t1 = PurchaseTransaction.builder()
                .description("Transaction 1")
                .transactionDate(LocalDate.now())
                .purchaseAmount(new BigDecimal("10.00"))
                .build();

        PurchaseTransaction t2 = PurchaseTransaction.builder()
                .description("Transaction 2")
                .transactionDate(LocalDate.now())
                .purchaseAmount(new BigDecimal("20.00"))
                .build();

        PurchaseTransaction saved1 = repository.save(t1);
        PurchaseTransaction saved2 = repository.save(t2);

        assertThat(saved1.getId()).isNotEqualTo(saved2.getId());
    }

    @Test
    @DisplayName("should return empty when transaction not found")
    void shouldReturnEmptyWhenNotFound() {
        Optional<PurchaseTransaction> found = repository.findById(UUID.randomUUID());
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("should persist description up to 50 characters")
    void shouldPersistDescriptionUpTo50Characters() {
        String maxDescription = "A".repeat(50);
        PurchaseTransaction transaction = PurchaseTransaction.builder()
                .description(maxDescription)
                .transactionDate(LocalDate.now())
                .purchaseAmount(new BigDecimal("1.00"))
                .build();

        PurchaseTransaction saved = repository.save(transaction);
        assertThat(saved.getDescription()).hasSize(50);
    }

    @Test
    @DisplayName("should persist minimum valid amount")
    void shouldPersistMinimumValidAmount() {
        PurchaseTransaction transaction = PurchaseTransaction.builder()
                .description("Min amount test")
                .transactionDate(LocalDate.now())
                .purchaseAmount(new BigDecimal("0.01"))
                .build();

        PurchaseTransaction saved = repository.save(transaction);
        assertThat(saved.getPurchaseAmount()).isEqualByComparingTo("0.01");
    }

    @Test
    @DisplayName("should persist large purchase amounts")
    void shouldPersistLargeAmounts() {
        PurchaseTransaction transaction = PurchaseTransaction.builder()
                .description("Large amount purchase")
                .transactionDate(LocalDate.now())
                .purchaseAmount(new BigDecimal("999999.99"))
                .build();

        PurchaseTransaction saved = repository.save(transaction);
        assertThat(saved.getPurchaseAmount()).isEqualByComparingTo("999999.99");
    }
}
