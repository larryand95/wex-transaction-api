package com.wexinc.transaction.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wexinc.transaction.domain.model.ConvertedTransactionResponse;
import com.wexinc.transaction.domain.model.CreateTransactionRequest;
import com.wexinc.transaction.domain.model.TransactionResponse;
import com.wexinc.transaction.exception.CurrencyConversionException;
import com.wexinc.transaction.exception.GlobalExceptionHandler;
import com.wexinc.transaction.exception.TransactionNotFoundException;
import com.wexinc.transaction.service.PurchaseTransactionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PurchaseTransactionController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("PurchaseTransactionController Tests")
class PurchaseTransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PurchaseTransactionService transactionService;

    @Nested
    @DisplayName("POST /api/v1/transactions")
    class CreateTransaction {

        @Test
        @DisplayName("should return 201 with transaction response on valid request")
        void shouldCreateTransaction() throws Exception {
            UUID id = UUID.randomUUID();
            CreateTransactionRequest request = CreateTransactionRequest.builder()
                    .description("Laptop purchase")
                    .transactionDate(LocalDate.of(2024, 6, 15))
                    .purchaseAmount(new BigDecimal("1299.99"))
                    .build();

            TransactionResponse response = TransactionResponse.builder()
                    .id(id)
                    .description("Laptop purchase")
                    .transactionDate(LocalDate.of(2024, 6, 15))
                    .purchaseAmount(new BigDecimal("1299.99"))
                    .build();

            when(transactionService.createTransaction(any())).thenReturn(response);

            mockMvc.perform(post("/api/v1/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(id.toString()))
                    .andExpect(jsonPath("$.description").value("Laptop purchase"))
                    .andExpect(jsonPath("$.transactionDate").value("2024-06-15"))
                    .andExpect(jsonPath("$.purchaseAmount").value(1299.99));
        }

        @Test
        @DisplayName("should return 400 when description is blank")
        void shouldReturn400WhenDescriptionIsBlank() throws Exception {
            CreateTransactionRequest request = CreateTransactionRequest.builder()
                    .description("")
                    .transactionDate(LocalDate.now())
                    .purchaseAmount(new BigDecimal("10.00"))
                    .build();

            mockMvc.perform(post("/api/v1/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.violations.description").exists());
        }

        @Test
        @DisplayName("should return 400 when description exceeds 50 characters")
        void shouldReturn400WhenDescriptionTooLong() throws Exception {
            CreateTransactionRequest request = CreateTransactionRequest.builder()
                    .description("A".repeat(51))
                    .transactionDate(LocalDate.now())
                    .purchaseAmount(new BigDecimal("10.00"))
                    .build();

            mockMvc.perform(post("/api/v1/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.violations.description").exists());
        }

        @Test
        @DisplayName("should return 400 when transaction date is null")
        void shouldReturn400WhenDateIsNull() throws Exception {
            CreateTransactionRequest request = CreateTransactionRequest.builder()
                    .description("Test")
                    .transactionDate(null)
                    .purchaseAmount(new BigDecimal("10.00"))
                    .build();

            mockMvc.perform(post("/api/v1/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.violations.transactionDate").exists());
        }

        @Test
        @DisplayName("should return 400 when purchase amount is zero")
        void shouldReturn400WhenAmountIsZero() throws Exception {
            CreateTransactionRequest request = CreateTransactionRequest.builder()
                    .description("Test purchase")
                    .transactionDate(LocalDate.now())
                    .purchaseAmount(BigDecimal.ZERO)
                    .build();

            mockMvc.perform(post("/api/v1/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.violations.purchaseAmount").exists());
        }

        @Test
        @DisplayName("should return 400 when purchase amount is negative")
        void shouldReturn400WhenAmountIsNegative() throws Exception {
            CreateTransactionRequest request = CreateTransactionRequest.builder()
                    .description("Test purchase")
                    .transactionDate(LocalDate.now())
                    .purchaseAmount(new BigDecimal("-5.00"))
                    .build();

            mockMvc.perform(post("/api/v1/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when purchase amount is null")
        void shouldReturn400WhenAmountIsNull() throws Exception {
            String body = """
                    {
                        "description": "Test",
                        "transactionDate": "2024-06-15"
                    }
                    """;

            mockMvc.perform(post("/api/v1/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.violations.purchaseAmount").exists());
        }

        @Test
        @DisplayName("should return 400 when amount has more than 2 decimal places")
        void shouldReturn400WhenAmountHasTooManyDecimals() throws Exception {
            CreateTransactionRequest request = CreateTransactionRequest.builder()
                    .description("Test")
                    .transactionDate(LocalDate.now())
                    .purchaseAmount(new BigDecimal("10.999"))
                    .build();

            mockMvc.perform(post("/api/v1/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/transactions/{id}")
    class GetTransactionInCurrency {

        @Test
        @DisplayName("should return 200 with converted transaction")
        void shouldReturnConvertedTransaction() throws Exception {
            UUID id = UUID.randomUUID();
            ConvertedTransactionResponse response = ConvertedTransactionResponse.builder()
                    .id(id)
                    .description("Hotel stay")
                    .transactionDate(LocalDate.of(2024, 5, 10))
                    .originalAmount(new BigDecimal("200.00"))
                    .exchangeRate(new BigDecimal("1.3641"))
                    .convertedAmount(new BigDecimal("272.82"))
                    .currency("Dollar")
                    .country("Canada")
                    .build();

            when(transactionService.getTransactionInCurrency(id, "Canada", "Dollar"))
                    .thenReturn(response);

            mockMvc.perform(get("/api/v1/transactions/{id}", id)
                            .param("country", "Canada")
                            .param("currency", "Dollar"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id.toString()))
                    .andExpect(jsonPath("$.description").value("Hotel stay"))
                    .andExpect(jsonPath("$.transactionDate").value("2024-05-10"))
                    .andExpect(jsonPath("$.originalAmount").value(200.00))
                    .andExpect(jsonPath("$.exchangeRate").value(1.3641))
                    .andExpect(jsonPath("$.convertedAmount").value(272.82))
                    .andExpect(jsonPath("$.currency").value("Dollar"))
                    .andExpect(jsonPath("$.country").value("Canada"));
        }

        @Test
        @DisplayName("should return 404 when transaction not found")
        void shouldReturn404WhenNotFound() throws Exception {
            UUID id = UUID.randomUUID();

            when(transactionService.getTransactionInCurrency(any(), any(), any()))
                    .thenThrow(new TransactionNotFoundException(id));

            mockMvc.perform(get("/api/v1/transactions/{id}", id)
                            .param("country", "Canada")
                            .param("currency", "Dollar"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("Transaction Not Found"));
        }

        @Test
        @DisplayName("should return 422 when currency conversion is unavailable")
        void shouldReturn422WhenCurrencyConversionUnavailable() throws Exception {
            UUID id = UUID.randomUUID();

            when(transactionService.getTransactionInCurrency(any(), any(), any()))
                    .thenThrow(new CurrencyConversionException("Zimbabwe", "Dollar"));

            mockMvc.perform(get("/api/v1/transactions/{id}", id)
                            .param("country", "Zimbabwe")
                            .param("currency", "Dollar"))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.title").value("Currency Conversion Unavailable"))
                    .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Zimbabwe")));
        }
    }
}
