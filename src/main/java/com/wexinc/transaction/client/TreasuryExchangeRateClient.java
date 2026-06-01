package com.wexinc.transaction.client;

import com.wexinc.transaction.domain.model.TreasuryExchangeRateResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Slf4j
@Component
public class TreasuryExchangeRateClient {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String RATES_ENDPOINT = "/v1/accounting/od/rates_of_exchange";

    private final WebClient webClient;

    public TreasuryExchangeRateClient(
            WebClient.Builder webClientBuilder,
            @Value("${treasury.api.base-url:https://api.fiscaldata.treasury.gov}") String baseUrl) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
    }

    /**
     * Fetch exchange rates for a given currency/country, filtering by date range.
     * Uses the Treasury Reporting Rates of Exchange API.
     *
     * @param country       the country name to filter by
     * @param currency      the currency name to filter by
     * @param purchaseDate  the purchase date; rate must be <= this date and within 6 months prior
     * @return optional containing the matching exchange rate data, or empty if none found
     */
    public Optional<TreasuryExchangeRateResponse.ExchangeRateData> getExchangeRate(
            String country, String currency, LocalDate purchaseDate) {

        LocalDate sixMonthsBefore = purchaseDate.minusMonths(6);

        String filterParam = buildFilterParam(country, currency, sixMonthsBefore, purchaseDate);

        log.debug("Fetching exchange rate for country={}, currency={}, date={}", country, currency, purchaseDate);

        String uri = UriComponentsBuilder.fromPath(RATES_ENDPOINT)
                .queryParam("fields", "country,currency,exchange_rate,record_date,effective_date")
                .queryParam("filter", filterParam)
                .queryParam("sort", "-record_date")
                .queryParam("page[size]", "1")
                .build()
                .toUriString();

        TreasuryExchangeRateResponse response = webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(TreasuryExchangeRateResponse.class)
                .onErrorResume(e -> {
                    log.error("Error fetching exchange rate from Treasury API: {}", e.getMessage(), e);
                    return Mono.empty();
                })
                .block();

        if (response == null || response.getData() == null || response.getData().isEmpty()) {
            log.info("No exchange rate found for country={}, currency={}, purchaseDate={}", country, currency, purchaseDate);
            return Optional.empty();
        }

        return Optional.of(response.getData().getFirst());
    }

    private String buildFilterParam(String country, String currency, LocalDate fromDate, LocalDate toDate) {
        return String.format(
                "country:eq:%s,currency:eq:%s,record_date:lte:%s,record_date:gte:%s",
                country,
                currency,
                toDate.format(DATE_FORMATTER),
                fromDate.format(DATE_FORMATTER)
        );
    }
}
