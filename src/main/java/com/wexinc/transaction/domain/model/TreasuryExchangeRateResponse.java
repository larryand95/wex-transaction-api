package com.wexinc.transaction.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TreasuryExchangeRateResponse {

    @JsonProperty("data")
    private List<ExchangeRateData> data;

    @JsonProperty("meta")
    private Meta meta;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ExchangeRateData {

        @JsonProperty("country")
        private String country;

        @JsonProperty("currency")
        private String currency;

        @JsonProperty("exchange_rate")
        private BigDecimal exchangeRate;

        @JsonProperty("record_date")
        private LocalDate recordDate;

        @JsonProperty("effective_date")
        private LocalDate effectiveDate;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Meta {

        @JsonProperty("count")
        private Integer count;

        @JsonProperty("total-count")
        private Integer totalCount;
    }
}
