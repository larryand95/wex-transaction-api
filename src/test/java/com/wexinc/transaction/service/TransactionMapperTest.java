package com.wexinc.transaction.service;

import com.wexinc.transaction.domain.entity.PurchaseTransaction;
import com.wexinc.transaction.domain.model.CreateTransactionRequest;
import com.wexinc.transaction.domain.model.TransactionMapper;
import com.wexinc.transaction.domain.model.TransactionMapperImpl;
import com.wexinc.transaction.domain.model.TransactionResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TransactionMapper Tests")
class TransactionMapperTest {

    private final TransactionMapper mapper = new TransactionMapperImpl();

    @Test
    @DisplayName("should map request to entity with rounded amount")
    void shouldMapRequestToEntityWithRounding() {
        CreateTransactionRequest request = CreateTransactionRequest.builder()
                .description("Coffee")
                .transactionDate(LocalDate.of(2024, 7, 4))
                .purchaseAmount(new BigDecimal("5.555"))
                .build();

        PurchaseTransaction entity = mapper.toEntity(request);

        assertThat(entity.getDescription()).isEqualTo("Coffee");
        assertThat(entity.getTransactionDate()).isEqualTo(LocalDate.of(2024, 7, 4));
        assertThat(entity.getPurchaseAmount()).isEqualByComparingTo("5.56");
        assertThat(entity.getId()).isNull();
        assertThat(entity.getCreatedAt()).isNull();
    }

    @Test
    @DisplayName("should map request to entity rounding down")
    void shouldMapRequestToEntityRoundingDown() {
        CreateTransactionRequest request = CreateTransactionRequest.builder()
                .description("Lunch")
                .transactionDate(LocalDate.now())
                .purchaseAmount(new BigDecimal("12.344"))
                .build();

        PurchaseTransaction entity = mapper.toEntity(request);

        assertThat(entity.getPurchaseAmount()).isEqualByComparingTo("12.34");
    }

    @Test
    @DisplayName("should map entity to response")
    void shouldMapEntityToResponse() {
        UUID id = UUID.randomUUID();
        PurchaseTransaction entity = PurchaseTransaction.builder()
                .id(id)
                .description("Groceries")
                .transactionDate(LocalDate.of(2024, 8, 1))
                .purchaseAmount(new BigDecimal("87.30"))
                .build();

        TransactionResponse response = mapper.toResponse(entity);

        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getDescription()).isEqualTo("Groceries");
        assertThat(response.getTransactionDate()).isEqualTo(LocalDate.of(2024, 8, 1));
        assertThat(response.getPurchaseAmount()).isEqualByComparingTo("87.30");
    }

    @Test
    @DisplayName("should not modify already rounded amount")
    void shouldNotModifyAlreadyRoundedAmount() {
        CreateTransactionRequest request = CreateTransactionRequest.builder()
                .description("Exact amount")
                .transactionDate(LocalDate.now())
                .purchaseAmount(new BigDecimal("10.00"))
                .build();

        PurchaseTransaction entity = mapper.toEntity(request);

        assertThat(entity.getPurchaseAmount()).isEqualByComparingTo("10.00");
    }
}
