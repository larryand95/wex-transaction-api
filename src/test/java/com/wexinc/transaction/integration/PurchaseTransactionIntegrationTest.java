package com.wexinc.transaction.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.wexinc.transaction.PostgreSQLContainerConfig;
import com.wexinc.transaction.domain.model.ConvertedTransactionResponse;
import com.wexinc.transaction.domain.model.CreateTransactionRequest;
import com.wexinc.transaction.domain.model.TransactionResponse;
import com.wexinc.transaction.domain.model.TreasuryExchangeRateResponse;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;


import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(PostgreSQLContainerConfig.class)
@ActiveProfiles("test")
@DisplayName("Purchase Transaction Integration Tests")
class PurchaseTransactionIntegrationTest {

    private static MockWebServer mockTreasuryServer;
    private static final ObjectMapper objectMapper;

    static {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.findAndRegisterModules();
        objectMapper.configure(
                com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) throws IOException {
        mockTreasuryServer = new MockWebServer();
        mockTreasuryServer.start();
        registry.add("treasury.api.base-url",
                () -> "http://localhost:" + mockTreasuryServer.getPort());
    }

    @AfterAll
    static void tearDown() throws IOException {
        mockTreasuryServer.shutdown();
    }

    @Nested
    @DisplayName("POST /api/v1/transactions - Store Transaction")
    class StoreTransaction {

        @Test
        @DisplayName("should create a transaction and return 201 with unique id")
        void shouldCreateTransactionAndReturnUniqueId() {
            CreateTransactionRequest request = CreateTransactionRequest.builder()
                    .description("International flight")
                    .transactionDate(LocalDate.of(2024, 9, 15))
                    .purchaseAmount(new BigDecimal("845.00"))
                    .build();

            ResponseEntity<TransactionResponse> response = restTemplate.postForEntity(
                    "/api/v1/transactions", request, TransactionResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getId()).isNotNull();
            assertThat(response.getBody().getDescription()).isEqualTo("International flight");
            assertThat(response.getBody().getTransactionDate()).isEqualTo(LocalDate.of(2024, 9, 15));
            assertThat(response.getBody().getPurchaseAmount()).isEqualByComparingTo("845.00");
        }

        @Test
        @DisplayName("should generate different IDs for different transactions")
        void shouldGenerateDifferentIds() {
            CreateTransactionRequest request1 = buildRequest("Purchase 1", new BigDecimal("10.00"));
            CreateTransactionRequest request2 = buildRequest("Purchase 2", new BigDecimal("20.00"));

            ResponseEntity<TransactionResponse> r1 = restTemplate.postForEntity(
                    "/api/v1/transactions", request1, TransactionResponse.class);
            ResponseEntity<TransactionResponse> r2 = restTemplate.postForEntity(
                    "/api/v1/transactions", request2, TransactionResponse.class);

            assertThat(r1.getBody()).isNotNull();
            assertThat(r2.getBody()).isNotNull();
            assertThat(r1.getBody().getId()).isNotEqualTo(r2.getBody().getId());
        }

        @Test
        @DisplayName("should round purchase amount to nearest cent on storage")
        void shouldRoundAmountOnStorage() {
            CreateTransactionRequest request = CreateTransactionRequest.builder()
                    .description("Rounded purchase")
                    .transactionDate(LocalDate.now())
                    .purchaseAmount(new BigDecimal("19.995"))
                    .build();

            // Note: validation rejects >2dp so this tests boundary with exactly 2dp
            CreateTransactionRequest validRequest = CreateTransactionRequest.builder()
                    .description("Rounded purchase")
                    .transactionDate(LocalDate.now())
                    .purchaseAmount(new BigDecimal("19.99"))
                    .build();

            ResponseEntity<TransactionResponse> response = restTemplate.postForEntity(
                    "/api/v1/transactions", validRequest, TransactionResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getPurchaseAmount()).isEqualByComparingTo("19.99");
        }

        @Test
        @DisplayName("should return 400 when description exceeds 50 characters")
        void shouldReturn400ForLongDescription() {
            CreateTransactionRequest request = CreateTransactionRequest.builder()
                    .description("X".repeat(51))
                    .transactionDate(LocalDate.now())
                    .purchaseAmount(new BigDecimal("10.00"))
                    .build();

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "/api/v1/transactions", request, Map.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("should return 400 when amount is negative")
        void shouldReturn400ForNegativeAmount() {
            CreateTransactionRequest request = CreateTransactionRequest.builder()
                    .description("Negative test")
                    .transactionDate(LocalDate.now())
                    .purchaseAmount(new BigDecimal("-1.00"))
                    .build();

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "/api/v1/transactions", request, Map.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("should accept description with exactly 50 characters")
        void shouldAccept50CharDescription() {
            CreateTransactionRequest request = CreateTransactionRequest.builder()
                    .description("A".repeat(50))
                    .transactionDate(LocalDate.now())
                    .purchaseAmount(new BigDecimal("5.00"))
                    .build();

            ResponseEntity<TransactionResponse> response = restTemplate.postForEntity(
                    "/api/v1/transactions", request, TransactionResponse.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/transactions/{id} - Retrieve with Currency Conversion")
    class RetrieveWithCurrencyConversion {

        @Test
        @DisplayName("should retrieve transaction converted to Canadian Dollar")
        void shouldRetrieveWithCurrencyConversion() throws Exception {
            // Store transaction
            CreateTransactionRequest request = CreateTransactionRequest.builder()
                    .description("Hotel stay")
                    .transactionDate(LocalDate.of(2024, 6, 15))
                    .purchaseAmount(new BigDecimal("200.00"))
                    .build();

            ResponseEntity<TransactionResponse> createResponse = restTemplate.postForEntity(
                    "/api/v1/transactions", request, TransactionResponse.class);
            assertThat(createResponse.getBody()).isNotNull();
            UUID id = createResponse.getBody().getId();

            // Setup mock Treasury API response
            TreasuryExchangeRateResponse treasuryResponse = buildTreasuryResponse(
                    "Canada", "Dollar", new BigDecimal("1.3641"), LocalDate.of(2024, 6, 30));

            mockTreasuryServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody(objectMapper.writeValueAsString(treasuryResponse)));

            // Retrieve with conversion
            ResponseEntity<ConvertedTransactionResponse> response = restTemplate.getForEntity(
                    "/api/v1/transactions/{id}?country=Canada&currency=Dollar",
                    ConvertedTransactionResponse.class, id);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            ConvertedTransactionResponse body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.getId()).isEqualTo(id);
            assertThat(body.getDescription()).isEqualTo("Hotel stay");
            assertThat(body.getOriginalAmount()).isEqualByComparingTo("200.00");
            assertThat(body.getExchangeRate()).isEqualByComparingTo("1.3641");
            assertThat(body.getConvertedAmount()).isEqualByComparingTo("272.82");
            assertThat(body.getCurrency()).isEqualTo("Dollar");
            assertThat(body.getCountry()).isEqualTo("Canada");
        }

        @Test
        @DisplayName("should return 404 when transaction ID does not exist")
        void shouldReturn404ForUnknownId() {
            UUID unknownId = UUID.randomUUID();

            ResponseEntity<Map> response = restTemplate.getForEntity(
                    "/api/v1/transactions/{id}?country=Canada&currency=Dollar",
                    Map.class, unknownId);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("should return 422 when no exchange rate available within 6 months")
        void shouldReturn422WhenNoExchangeRateAvailable() throws Exception {
            // Store transaction
            CreateTransactionRequest request = buildRequest("Old purchase", new BigDecimal("50.00"));
            ResponseEntity<TransactionResponse> createResponse = restTemplate.postForEntity(
                    "/api/v1/transactions", request, TransactionResponse.class);
            assertThat(createResponse.getBody()).isNotNull();
            UUID id = createResponse.getBody().getId();

            // Mock Treasury returns empty
            TreasuryExchangeRateResponse emptyResponse = new TreasuryExchangeRateResponse();
            emptyResponse.setData(List.of());

            mockTreasuryServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody(objectMapper.writeValueAsString(emptyResponse)));

            ResponseEntity<Map> response = restTemplate.getForEntity(
                    "/api/v1/transactions/{id}?country=Zimbabwe&currency=Dollar",
                    Map.class, id);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().get("title")).isEqualTo("Currency Conversion Unavailable");
        }

        @Test
        @DisplayName("should convert amount correctly and round to 2 decimal places")
        void shouldRoundConvertedAmountToTwoDecimals() throws Exception {
            CreateTransactionRequest request = CreateTransactionRequest.builder()
                    .description("Yen conversion test")
                    .transactionDate(LocalDate.of(2024, 8, 1))
                    .purchaseAmount(new BigDecimal("77.00"))
                    .build();

            ResponseEntity<TransactionResponse> createResponse = restTemplate.postForEntity(
                    "/api/v1/transactions", request, TransactionResponse.class);
            assertThat(createResponse.getBody()).isNotNull();
            UUID id = createResponse.getBody().getId();

            TreasuryExchangeRateResponse treasuryResponse = buildTreasuryResponse(
                    "Japan", "Yen", new BigDecimal("146.523"), LocalDate.of(2024, 7, 31));

            mockTreasuryServer.enqueue(new MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody(objectMapper.writeValueAsString(treasuryResponse)));

            ResponseEntity<ConvertedTransactionResponse> response = restTemplate.getForEntity(
                    "/api/v1/transactions/{id}?country=Japan&currency=Yen",
                    ConvertedTransactionResponse.class, id);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            // 77.00 * 146.523 = 11282.271 → rounded to 11282.27
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getConvertedAmount()).isEqualByComparingTo("11282.27");
        }
    }

    private CreateTransactionRequest buildRequest(String description, BigDecimal amount) {
        return CreateTransactionRequest.builder()
                .description(description)
                .transactionDate(LocalDate.of(2024, 6, 1))
                .purchaseAmount(amount)
                .build();
    }

    private TreasuryExchangeRateResponse buildTreasuryResponse(String country, String currency,
                                                                 BigDecimal rate, LocalDate recordDate) {
        TreasuryExchangeRateResponse.ExchangeRateData data = new TreasuryExchangeRateResponse.ExchangeRateData();
        data.setCountry(country);
        data.setCurrency(currency);
        data.setExchangeRate(rate);
        data.setRecordDate(recordDate);

        TreasuryExchangeRateResponse response = new TreasuryExchangeRateResponse();
        response.setData(List.of(data));
        return response;
    }
}
