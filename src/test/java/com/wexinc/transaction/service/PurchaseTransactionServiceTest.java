package com.wexinc.transaction.service;

import com.wexinc.transaction.client.TreasuryExchangeRateClient;
import com.wexinc.transaction.domain.entity.PurchaseTransaction;
import com.wexinc.transaction.domain.model.*;
import com.wexinc.transaction.exception.CurrencyConversionException;
import com.wexinc.transaction.exception.TransactionNotFoundException;
import com.wexinc.transaction.repository.PurchaseTransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PurchaseTransactionService Tests")
class PurchaseTransactionServiceTest {

    @Mock
    private PurchaseTransactionRepository repository;

    @Mock
    private TreasuryExchangeRateClient exchangeRateClient;

    @Mock
    private TransactionMapper transactionMapper;

    @InjectMocks
    private PurchaseTransactionService service;

    @Nested
    @DisplayName("createTransaction")
    class CreateTransaction {

        @Test
        @DisplayName("should create and return transaction response")
        void shouldCreateTransaction() {
            CreateTransactionRequest request = CreateTransactionRequest.builder()
                    .description("Office supplies")
                    .transactionDate(LocalDate.of(2024, 5, 20))
                    .purchaseAmount(new BigDecimal("49.99"))
                    .build();

            PurchaseTransaction entity = buildTransaction(UUID.randomUUID(), "Office supplies",
                    LocalDate.of(2024, 5, 20), new BigDecimal("49.99"));

            TransactionResponse expectedResponse = TransactionResponse.builder()
                    .id(entity.getId())
                    .description("Office supplies")
                    .transactionDate(LocalDate.of(2024, 5, 20))
                    .purchaseAmount(new BigDecimal("49.99"))
                    .build();

            when(transactionMapper.toEntity(request)).thenReturn(entity);
            when(repository.save(entity)).thenReturn(entity);
            when(transactionMapper.toResponse(entity)).thenReturn(expectedResponse);

            TransactionResponse result = service.createTransaction(request);

            assertThat(result).isEqualTo(expectedResponse);
            verify(repository).save(entity);
        }

        @Test
        @DisplayName("should delegate amount rounding to mapper")
        void shouldDelegateRoundingToMapper() {
            CreateTransactionRequest request = CreateTransactionRequest.builder()
                    .description("Rounded amount")
                    .transactionDate(LocalDate.now())
                    .purchaseAmount(new BigDecimal("10.555"))
                    .build();

            PurchaseTransaction entity = buildTransaction(UUID.randomUUID(), "Rounded amount",
                    LocalDate.now(), new BigDecimal("10.56"));

            when(transactionMapper.toEntity(request)).thenReturn(entity);
            when(repository.save(entity)).thenReturn(entity);
            when(transactionMapper.toResponse(entity)).thenReturn(mock(TransactionResponse.class));

            service.createTransaction(request);

            verify(transactionMapper).toEntity(request);
        }
    }

    @Nested
    @DisplayName("getTransactionInCurrency")
    class GetTransactionInCurrency {

        @Test
        @DisplayName("should return converted transaction when exchange rate is available")
        void shouldReturnConvertedTransaction() {
            UUID id = UUID.randomUUID();
            LocalDate purchaseDate = LocalDate.of(2024, 6, 15);
            BigDecimal purchaseAmount = new BigDecimal("100.00");
            BigDecimal exchangeRate = new BigDecimal("1.3641");

            PurchaseTransaction transaction = buildTransaction(id, "Flight booking", purchaseDate, purchaseAmount);

            TreasuryExchangeRateResponse.ExchangeRateData rateData =
                    new TreasuryExchangeRateResponse.ExchangeRateData();
            rateData.setCountry("Canada");
            rateData.setCurrency("Dollar");
            rateData.setExchangeRate(exchangeRate);
            rateData.setRecordDate(LocalDate.of(2024, 6, 30));

            when(repository.findById(id)).thenReturn(Optional.of(transaction));
            when(exchangeRateClient.getExchangeRate("Canada", "Dollar", purchaseDate))
                    .thenReturn(Optional.of(rateData));

            ConvertedTransactionResponse result = service.getTransactionInCurrency(id, "Canada", "Dollar");

            assertThat(result.getId()).isEqualTo(id);
            assertThat(result.getDescription()).isEqualTo("Flight booking");
            assertThat(result.getTransactionDate()).isEqualTo(purchaseDate);
            assertThat(result.getOriginalAmount()).isEqualByComparingTo("100.00");
            assertThat(result.getExchangeRate()).isEqualByComparingTo("1.3641");
            assertThat(result.getConvertedAmount()).isEqualByComparingTo("136.41");
            assertThat(result.getCurrency()).isEqualTo("Dollar");
            assertThat(result.getCountry()).isEqualTo("Canada");
        }

        @Test
        @DisplayName("should round converted amount to two decimal places")
        void shouldRoundConvertedAmountToTwoDecimals() {
            UUID id = UUID.randomUUID();
            LocalDate purchaseDate = LocalDate.of(2024, 6, 15);

            PurchaseTransaction transaction = buildTransaction(id, "Test", purchaseDate, new BigDecimal("100.00"));

            TreasuryExchangeRateResponse.ExchangeRateData rateData =
                    new TreasuryExchangeRateResponse.ExchangeRateData();
            rateData.setCountry("Japan");
            rateData.setCurrency("Yen");
            rateData.setExchangeRate(new BigDecimal("149.876"));
            rateData.setRecordDate(purchaseDate);

            when(repository.findById(id)).thenReturn(Optional.of(transaction));
            when(exchangeRateClient.getExchangeRate("Japan", "Yen", purchaseDate))
                    .thenReturn(Optional.of(rateData));

            ConvertedTransactionResponse result = service.getTransactionInCurrency(id, "Japan", "Yen");

            // 100.00 * 149.876 = 14987.6, rounded to 14987.60
            assertThat(result.getConvertedAmount()).isEqualByComparingTo("14987.60");
        }

        @Test
        @DisplayName("should throw TransactionNotFoundException when transaction does not exist")
        void shouldThrowWhenTransactionNotFound() {
            UUID id = UUID.randomUUID();

            when(repository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getTransactionInCurrency(id, "Canada", "Dollar"))
                    .isInstanceOf(TransactionNotFoundException.class)
                    .hasMessageContaining(id.toString());
        }

        @Test
        @DisplayName("should throw CurrencyConversionException when no exchange rate available")
        void shouldThrowWhenNoExchangeRate() {
            UUID id = UUID.randomUUID();
            LocalDate purchaseDate = LocalDate.of(2020, 1, 1);

            PurchaseTransaction transaction = buildTransaction(id, "Old purchase", purchaseDate, new BigDecimal("50.00"));

            when(repository.findById(id)).thenReturn(Optional.of(transaction));
            when(exchangeRateClient.getExchangeRate(any(), any(), any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getTransactionInCurrency(id, "Zimbabwe", "Dollar"))
                    .isInstanceOf(CurrencyConversionException.class)
                    .hasMessageContaining("Zimbabwe")
                    .hasMessageContaining("Dollar");
        }

        @Test
        @DisplayName("should pass correct purchase date to exchange rate client")
        void shouldPassCorrectDateToExchangeRateClient() {
            UUID id = UUID.randomUUID();
            LocalDate purchaseDate = LocalDate.of(2024, 3, 10);

            PurchaseTransaction transaction = buildTransaction(id, "Test", purchaseDate, new BigDecimal("25.00"));

            when(repository.findById(id)).thenReturn(Optional.of(transaction));
            when(exchangeRateClient.getExchangeRate("Mexico", "Peso", purchaseDate))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getTransactionInCurrency(id, "Mexico", "Peso"))
                    .isInstanceOf(CurrencyConversionException.class);

            verify(exchangeRateClient).getExchangeRate("Mexico", "Peso", purchaseDate);
        }
    }

    private PurchaseTransaction buildTransaction(UUID id, String description,
                                                  LocalDate date, BigDecimal amount) {
        return PurchaseTransaction.builder()
                .id(id)
                .description(description)
                .transactionDate(date)
                .purchaseAmount(amount)
                .build();
    }
}
