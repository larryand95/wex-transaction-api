package com.wexinc.transaction.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.wexinc.transaction.client.TreasuryExchangeRateClient;
import com.wexinc.transaction.domain.model.TreasuryExchangeRateResponse;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TreasuryExchangeRateClient Tests")
class TreasuryExchangeRateClientTest {

    private MockWebServer mockWebServer;
    private TreasuryExchangeRateClient client;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        String baseUrl = mockWebServer.url("/").toString().replaceAll("/$", "");
        client = new TreasuryExchangeRateClient(WebClient.builder(), baseUrl);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    @DisplayName("should return exchange rate when found")
    void shouldReturnExchangeRateWhenFound() throws Exception {
        TreasuryExchangeRateResponse mockResponse = buildMockResponse("Canada", "Dollar",
                new BigDecimal("1.3641"), LocalDate.of(2024, 6, 30));

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(objectMapper.writeValueAsString(mockResponse)));

        Optional<TreasuryExchangeRateResponse.ExchangeRateData> result =
                client.getExchangeRate("Canada", "Dollar", LocalDate.of(2024, 6, 15));

        assertThat(result).isPresent();
        assertThat(result.get().getCountry()).isEqualTo("Canada");
        assertThat(result.get().getCurrency()).isEqualTo("Dollar");
        assertThat(result.get().getExchangeRate()).isEqualByComparingTo("1.3641");
    }

    @Test
    @DisplayName("should return empty when no data returned")
    void shouldReturnEmptyWhenNoData() throws Exception {
        TreasuryExchangeRateResponse emptyResponse = new TreasuryExchangeRateResponse();
        emptyResponse.setData(java.util.Collections.emptyList());

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(objectMapper.writeValueAsString(emptyResponse)));

        Optional<TreasuryExchangeRateResponse.ExchangeRateData> result =
                client.getExchangeRate("Zimbabwe", "Dollar", LocalDate.of(2020, 1, 1));

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should return empty on server error")
    void shouldReturnEmptyOnServerError() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        Optional<TreasuryExchangeRateResponse.ExchangeRateData> result =
                client.getExchangeRate("Canada", "Dollar", LocalDate.of(2024, 6, 15));

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should include correct filter parameters in request")
    void shouldIncludeCorrectFilterParameters() throws Exception {
        TreasuryExchangeRateResponse mockResponse = buildMockResponse("Mexico", "Peso",
                new BigDecimal("17.23"), LocalDate.of(2024, 3, 31));

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(objectMapper.writeValueAsString(mockResponse)));

        client.getExchangeRate("Mexico", "Peso", LocalDate.of(2024, 6, 15));

        RecordedRequest request = mockWebServer.takeRequest();
        String requestPath = request.getPath();

        assertThat(requestPath).contains("country%3Aeq%3AMexico");
        assertThat(requestPath).contains("currency%3Aeq%3APeso");
        assertThat(requestPath).contains("record_date%3Alte%3A2024-06-15");
        assertThat(requestPath).contains("record_date%3Agte%3A2023-12-15");
        assertThat(requestPath).contains("sort=-record_date");
        assertThat(requestPath).contains("page%5Bsize%5D=1");
    }

    @Test
    @DisplayName("should use purchase date as upper bound and 6-months-prior as lower bound")
    void shouldUseSixMonthWindowForFilter() throws Exception {
        TreasuryExchangeRateResponse mockResponse = buildMockResponse("Japan", "Yen",
                new BigDecimal("149.23"), LocalDate.of(2024, 1, 31));

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(objectMapper.writeValueAsString(mockResponse)));

        LocalDate purchaseDate = LocalDate.of(2024, 7, 31);
        client.getExchangeRate("Japan", "Yen", purchaseDate);

        RecordedRequest request = mockWebServer.takeRequest();
        String path = request.getPath();

        assertThat(path).contains("2024-07-31"); // upper bound = purchase date
        assertThat(path).contains("2024-01-31"); // lower bound = 6 months prior
    }

    private TreasuryExchangeRateResponse buildMockResponse(String country, String currency,
                                                             BigDecimal rate, LocalDate recordDate) {
        TreasuryExchangeRateResponse.ExchangeRateData data = new TreasuryExchangeRateResponse.ExchangeRateData();
        data.setCountry(country);
        data.setCurrency(currency);
        data.setExchangeRate(rate);
        data.setRecordDate(recordDate);

        TreasuryExchangeRateResponse response = new TreasuryExchangeRateResponse();
        response.setData(java.util.List.of(data));
        return response;
    }
}
